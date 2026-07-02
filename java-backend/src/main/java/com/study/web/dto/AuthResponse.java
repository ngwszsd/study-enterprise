package com.study.web.dto;

/** 登陆响应:令牌 + 类型 + 过期秒数。 */
public record AuthResponse(String token, String tokenType, long expiresIn) {
}
