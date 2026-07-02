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

/** 认证接口:注册、登陆、当前用户。 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService,
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): UserResponse {
        val user = authService.register(request.username, request.password)
        return UserResponse(user.id, user.username)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        val token = authService.login(request.username, request.password)
        return AuthResponse(token, "Bearer", jwtService.expiresInSeconds)
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal user: AuthUser): UserResponse = UserResponse(user.id, user.username)
}
