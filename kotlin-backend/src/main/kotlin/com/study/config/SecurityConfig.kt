package com.study.config

import com.study.security.JwtAuthenticationFilter
import com.study.security.JwtService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * 安全配置:声明哪些接口要登陆/放行,装配 JWT 过滤器、密码加密器、CORS,统一 401/403 JSON。
 *
 * 【前端类比】authorizeHttpRequests = 路由守卫的服务端版;无状态 = 不用 session,靠每次请求带 JWT。
 * Kotlin 用尾随 lambda(`http.csrf { it.disable() }`),Java 用方法引用,是同一套 DSL。
 */
// @Configuration: 声明这是配置类,里面的 @Bean 方法会参与 Spring 容器装配。
@Configuration
class SecurityConfig(private val jwtService: JwtService) {

    // @Bean: 把返回的 SecurityFilterChain 注册进 Spring 容器,由 Spring Security 自动使用。
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                    .requestMatchers("/api/notes/internal/**").permitAll()
                    .requestMatchers("/actuator/health", "/ws/**", "/api/sse/**").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, res, _ -> writeError(res, 401, "UNAUTHORIZED", "未认证或令牌无效") }
                    .accessDeniedHandler { _, res, _ -> writeError(res, 403, "FORBIDDEN", "无权限") }
            }
            .addFilterBefore(JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    // @Bean: PasswordEncoder 是一个可注入依赖,AuthService 会用它做 BCrypt 加密/校验。
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    // @Bean: CORS 配置也作为 Bean 提供给 Spring Security。
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf("http://localhost:15173")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    private fun writeError(response: HttpServletResponse, status: Int, code: String, message: String) {
        response.status = status
        response.contentType = "application/json;charset=UTF-8"
        response.writer.write("""{"code":"$code","message":"$message"}""")
    }
}
