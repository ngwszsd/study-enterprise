package com.study.web

import com.study.security.AuthUser
import com.study.service.ArticleService
import com.study.web.dto.ArticleRequest
import com.study.web.dto.ArticleResponse
import com.study.web.dto.CategoryCount
import com.study.web.dto.PageResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 文章接口:分页列表/详情/新建/更新/删除。均需登陆;写操作需作者本人。
 *
 * @RestController 返回 JSON;@RequestMapping 给所有函数统一加 /api/articles 前缀。
 */
@RestController
@RequestMapping("/api/articles")
class ArticleController(private val articleService: ArticleService) {

    // @GetMapping: 处理 GET /api/articles;@RequestParam 读取 query 参数。
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) keyword: String?,
    ): PageResponse<ArticleResponse> = articleService.list(keyword, page, size)

    @GetMapping("/stats")
    fun stats(): List<CategoryCount> = articleService.stats()

    // @PathVariable: 读取路径里的 {id},例如 GET /api/articles/1。
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ArticleResponse = articleService.get(id)

    // @RequestBody 读取 JSON 正文;@AuthenticationPrincipal 读取当前登录用户。
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: ArticleRequest, @AuthenticationPrincipal user: AuthUser): ArticleResponse =
        articleService.create(request, user.id)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: ArticleRequest,
        @AuthenticationPrincipal user: AuthUser,
    ): ArticleResponse = articleService.update(id, request, user.id)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, @AuthenticationPrincipal user: AuthUser) = articleService.delete(id, user.id)
}
