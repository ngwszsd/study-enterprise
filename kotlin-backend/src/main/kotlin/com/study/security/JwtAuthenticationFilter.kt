package com.study.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 解析 Authorization: Bearer <token>,校验通过则把 AuthUser 放入 SecurityContext。
 * 由 SecurityConfig 手动 new,避免被 Boot 自动注册进 servlet 过滤链。
 */
class JwtAuthenticationFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            try {
                val claims = jwtService.parse(header.substring(7))
                val principal = AuthUser(claims.subject.toLong(), claims.get("username", String::class.java))
                val authentication = UsernamePasswordAuthenticationToken(
                    principal, null, listOf(SimpleGrantedAuthority("ROLE_USER")),
                )
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            } catch (e: Exception) {
                // 令牌无效/过期:交由安全链返回 401
                SecurityContextHolder.clearContext()
            }
        }
        filterChain.doFilter(request, response)
    }
}
