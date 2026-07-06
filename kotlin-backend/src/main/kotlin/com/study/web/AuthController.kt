package com.study.web

import com.study.security.AuthUser
import com.study.security.JwtService
import com.study.service.AuthService
import com.study.web.dto.AuthResponse
import com.study.web.dto.LoginRequest
import com.study.web.dto.RegisterRequest
import com.study.web.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 认证接口:注册、登陆、当前用户(在 /api/auth 下)。
 *
 * 【前端类比】相当于 frontend/src/api/auth.ts 的服务端另一端。Controller 只管 HTTP,业务交给 Service。
 * (与 Java 侧同结构;差异只在语言:Kotlin 用主构造器注入、表达式函数体。)
 */
// @RestController: @Controller + @ResponseBody,函数返回对象会自动序列化成 JSON。
// @RequestMapping: 给这个 Controller 下所有接口统一加 /api/auth 前缀。
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService,
) {

    // @PostMapping: 处理 POST /api/auth/register;@ResponseStatus 指定成功时返回 201。
    // @Valid + @RequestBody: 把请求 JSON 绑定成 RegisterRequest,并执行 DTO 上的校验注解。
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): UserResponse {
        val user = authService.register(request.username, request.password)
        return UserResponse(user.id, user.username)
    }

    // @PostMapping: 处理 POST /api/auth/login。
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        val token = authService.login(request.username, request.password)
        return AuthResponse(token, "Bearer", jwtService.expiresInSeconds)
    }

    // @AuthenticationPrincipal: 从 Spring SecurityContext 取当前登录用户,不需要前端传 userId。
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: AuthUser): UserResponse = UserResponse(user.id, user.username)
}
