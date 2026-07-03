package com.study.web.dto;

import java.time.LocalDateTime;

public record NoteResponse(
        Long id,
        String title,
        Long ownerId,
        String ownerUsername,
        String role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
