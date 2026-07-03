package com.study.web.dto;

public record NoteMemberResponse(
        Long userId,
        String username,
        String role
) {
}
