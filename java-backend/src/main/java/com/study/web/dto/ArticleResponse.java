package com.study.web.dto;

import java.time.LocalDateTime;

/** 文章响应;coverImageUrl 为按 key 现算的预签名 GET;viewCount 来自 Redis。 */
public record ArticleResponse(
        Long id,
        String title,
        String content,
        String category,
        String coverImageKey,
        String coverImageUrl,
        Long authorId,
        String authorUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long viewCount) {
}
