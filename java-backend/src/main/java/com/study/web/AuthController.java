package com.study.web;

import com.study.domain.User;
import com.study.security.AuthUser;
import com.study.security.JwtService;
import com.study.service.AuthService;
import com.study.web.dto.AuthResponse;
import com.study.web.dto.LoginRequest;
import com.study.web.dto.RegisterRequest;
import com.study.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口:注册、登陆、当前用户(/api/auth/**)。
 *
 * 【前端类比】相当于 frontend/src/api/auth.ts 的服务端另一端 —— 你前端的 login()/register()/me()
 * 调的就是这里的方法。Controller 只管 HTTP(URL、方法、参数校验、返回),真正的业务交给 Service。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.username(), request.password());
        return new UserResponse(user.getId(), user.getUsername());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.username(), request.password());
        return new AuthResponse(token, "Bearer", jwtService.getExpiresInSeconds());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return new UserResponse(user.id(), user.username());
    }
}
