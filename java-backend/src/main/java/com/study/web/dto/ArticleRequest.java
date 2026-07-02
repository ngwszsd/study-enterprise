package com.study.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 文章创建/更新请求。 */
public record ArticleRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        @Size(max = 50) String category,
        @Size(max = 255) String coverImageKey) {
}
