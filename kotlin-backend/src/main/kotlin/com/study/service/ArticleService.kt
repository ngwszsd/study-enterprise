package com.study.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.extension.plugins.pagination.Page
import com.study.cache.RedisArticleCache
import com.study.domain.Article
import com.study.event.ArticleCreatedEvent
import com.study.exception.ForbiddenException
import com.study.exception.ResourceNotFoundException
import com.study.mapper.ArticleMapper
import com.study.mapper.UserMapper
import com.study.storage.StorageService
import com.study.web.dto.ArticleRequest
import com.study.web.dto.ArticleResponse
import com.study.web.dto.CategoryCount
import com.study.web.dto.PageResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 文章业务:CRUD + 分页搜索 + 作者归属校验 + Redis 缓存/浏览量 + 发布领域事件。
 *
 * 【前端类比】业务层(像业务 hook):@Transactional 事务、Redis 缓存读、发 ArticleCreatedEvent 让 WS/SSE
 * 实时推送(发布-订阅,类似前端事件总线)。逻辑与 Java 侧一致,写法更简洁(空安全 ?.、apply、data class)。
 */
@Service
class ArticleService(
    private val articleMapper: ArticleMapper,
    private val userMapper: UserMapper,
    private val storageService: StorageService,
    private val cache: RedisArticleCache,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional(readOnly = true)
    fun list(keyword: String?, page: Int, size: Int): PageResponse<ArticleResponse> {
        // MyBatis-Plus 分页 current 从 1 开始;对外仍用 0 基页码
        val mpPage = Page<Article>((page + 1).toLong(), size.toLong())
        val query = QueryWrapper<Article>()
        if (!keyword.isNullOrBlank()) {
            query.like("title", keyword.trim())
        }
        query.orderByDesc("created_at")
        articleMapper.selectPage(mpPage, query)

        val authorIds = mpPage.records.mapNotNull { it.authorId }.distinct()
        val usernames: Map<Long, String> =
            if (authorIds.isEmpty()) emptyMap()
            else userMapper.selectBatchIds(authorIds).associate { it.id!! to (it.username ?: "") }

        val items = mpPage.records.map { toResponse(it, usernames[it.authorId], cache.getViews(it.id!!)) }
        return PageResponse(items, page, size, mpPage.total, mpPage.pages.toInt())
    }

    @Transactional(readOnly = true)
    fun get(id: Long): ArticleResponse {
        val article = cache.get(id)
            ?: (articleMapper.selectById(id)?.also { cache.put(it) }
                ?: throw ResourceNotFoundException("文章不存在"))
        val views = cache.incrementViews(id)
        return toResponse(article, usernameOf(article.authorId), views)
    }

    @Transactional
    fun create(request: ArticleRequest, authorId: Long): ArticleResponse {
        val article = Article().apply {
            title = request.title
            content = request.content
            category = request.category
            coverImageKey = request.coverImageKey
            this.authorId = authorId
        }
        articleMapper.insert(article)
        val author = usernameOf(authorId)
        eventPublisher.publishEvent(ArticleCreatedEvent(article.id, article.title, author))
        return toResponse(article, author, 0L)
    }

    @Transactional
    fun update(id: Long, request: ArticleRequest, currentUserId: Long): ArticleResponse {
        val article = articleMapper.selectById(id) ?: throw ResourceNotFoundException("文章不存在")
        ensureOwner(article, currentUserId)
        article.apply {
            title = request.title
            content = request.content
            category = request.category
            coverImageKey = request.coverImageKey
        }
        articleMapper.updateById(article)
        cache.evict(id)
        return toResponse(article, usernameOf(article.authorId), cache.getViews(id))
    }

    @Transactional
    fun delete(id: Long, currentUserId: Long) {
        val article = articleMapper.selectById(id) ?: throw ResourceNotFoundException("文章不存在")
        ensureOwner(article, currentUserId)
        articleMapper.deleteById(id)
        cache.evict(id)
        article.coverImageKey?.let { storageService.delete(it) } // 删文章时清理 MinIO 封面对象
    }

    /** 按分类统计文章数(手写 SQL)。 */
    @Transactional(readOnly = true)
    fun stats(): List<CategoryCount> = articleMapper.countByCategory()

    private fun ensureOwner(article: Article, userId: Long) {
        if (article.authorId != userId) {
            throw ForbiddenException("只能操作自己的文章")
        }
    }

    private fun usernameOf(userId: Long?): String? =
        userId?.let { userMapper.selectById(it)?.username }

    private fun toResponse(a: Article, authorUsername: String?, viewCount: Long): ArticleResponse {
        val coverUrl = a.coverImageKey?.let { storageService.presignedGetUrl(it) }
        return ArticleResponse(
            a.id, a.title, a.content, a.category, a.coverImageKey, coverUrl,
            a.authorId, authorUsername, a.createdAt, a.updatedAt, viewCount,
        )
    }
}
