package com.study.web.dto;

public record CollabTokenResponse(
        String token,
        long expiresIn,
        String docName,
        String role,
        String url
) {
}
