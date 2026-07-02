package com.study.web.dto;

import java.util.List;

/** 统一分页响应结构(page 为 0 基页码)。 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
