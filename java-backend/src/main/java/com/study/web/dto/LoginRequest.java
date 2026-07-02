package com.study.web.dto;

import jakarta.validation.constraints.NotBlank;

/** 登陆请求。 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
