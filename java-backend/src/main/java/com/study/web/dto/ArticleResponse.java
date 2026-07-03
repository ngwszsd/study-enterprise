package com.study.web.dto;

import java.time.LocalDateTime;

/**
 * 文章响应 DTO(返回给前端的数据形状)。
 *
 * 【前端类比】就是一个 TS interface —— 和 frontend/src/api/types.ts 里的 Article 一一对应。
 * 用 record(不可变),字段直接序列化成 JSON。coverImageUrl 是按 key 现算的 MinIO 预签名地址(可空);
 * viewCount 来自 Redis。
 */
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
