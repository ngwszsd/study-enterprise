package com.study.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class RegisterRequest(
    @field:NotBlank @field:Size(min = 3, max = 50) val username: String,
    @field:NotBlank @field:Size(min = 6, max = 100) val password: String,
)

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String,
)

data class AuthResponse(val token: String, val tokenType: String, val expiresIn: Long)

data class UserResponse(val id: Long?, val username: String?)

data class ArticleRequest(
    @field:NotBlank @field:Size(max = 200) val title: String,
    @field:NotBlank val content: String,
    @field:Size(max = 50) val category: String?,
    @field:Size(max = 255) val coverImageKey: String?,
)

data class ArticleResponse(
    val id: Long?,
    val title: String?,
    val content: String?,
    val category: String?,
    val coverImageKey: String?,
    val coverImageUrl: String?,
    val authorId: Long?,
    val authorUsername: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val viewCount: Long,
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class FileResponse(val key: String, val url: String?)

data class NoteRequest(
    @field:NotBlank @field:Size(max = 200) val title: String,
)

data class NoteResponse(
    val id: Long?,
    val title: String?,
    val ownerId: Long?,
    val ownerUsername: String?,
    val role: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)

data class NoteMemberRequest(
    @field:jakarta.validation.constraints.NotNull val userId: Long,
    @field:jakarta.validation.constraints.Pattern(
        regexp = "EDITOR|VIEWER",
        message = "角色只能是 EDITOR 或 VIEWER",
    )
    val role: String?,
)

data class NoteMemberResponse(
    val userId: Long?,
    val username: String?,
    val role: String?,
)

data class CollabTokenResponse(
    val token: String,
    val expiresIn: Long,
    val docName: String,
    val role: String,
    val url: String,
)

data class NoteDocumentStateRequest(val state: String?)

data class NoteDocumentStateResponse(val state: String)

/** 统一错误响应;errors 为 null 时不序列化。 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(val code: String, val message: String?, val errors: Map<String, String>? = null)

/** 分类统计投影(手写 SQL 的 GROUP BY 结果)。用 var 便于 MyBatis setter 映射。 */
class CategoryCount {
    var category: String? = null
    var count: Long = 0
}
