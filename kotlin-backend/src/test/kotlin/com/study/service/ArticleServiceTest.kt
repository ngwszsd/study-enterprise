package com.study.service

import com.study.cache.RedisArticleCache
import com.study.domain.Article
import com.study.domain.User
import com.study.exception.ForbiddenException
import com.study.exception.ResourceNotFoundException
import com.study.mapper.ArticleMapper
import com.study.mapper.UserMapper
import com.study.storage.StorageService
import com.study.web.dto.ArticleRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

/** ArticleService 单元测试(MockK):创建、作者归属校验、未找到、删除。 */
class ArticleServiceTest {

    private val articleMapper = mockk<ArticleMapper>()
    private val userMapper = mockk<UserMapper>()
    private val storageService = mockk<StorageService>()
    private val cache = mockk<RedisArticleCache>(relaxed = true)
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val articleService = ArticleService(articleMapper, userMapper, storageService, cache, eventPublisher)

    @Test
    fun `创建 返回含作者与0浏览量`() {
        every { articleMapper.insert(any<Article>()) } returns 1
        every { userMapper.selectById(1L) } returns User().apply {
            id = 1L
            username = "alice"
        }

        val resp = articleService.create(ArticleRequest("t", "c", "cat", null), 1L)

        assertEquals("t", resp.title)
        assertEquals(1L, resp.authorId)
        assertEquals("alice", resp.authorUsername)
        assertEquals(0L, resp.viewCount)
    }

    @Test
    fun `非作者更新 抛禁止`() {
        val article = Article().apply {
            id = 10L
            authorId = 1L
        }
        every { articleMapper.selectById(10L) } returns article

        assertThrows(ForbiddenException::class.java) {
            articleService.update(10L, ArticleRequest("t2", "c2", null, null), 2L)
        }
    }

    @Test
    fun `详情不存在 抛未找到`() {
        every { cache.get(99L) } returns null
        every { articleMapper.selectById(99L) } returns null

        assertThrows(ResourceNotFoundException::class.java) { articleService.get(99L) }
    }

    @Test
    fun `作者删除 执行删除并失效缓存`() {
        val article = Article().apply {
            id = 5L
            authorId = 1L
        }
        every { articleMapper.selectById(5L) } returns article
        every { articleMapper.deleteById(5L) } returns 1

        articleService.delete(5L, 1L)

        verify { articleMapper.deleteById(5L) }
        verify { cache.evict(5L) }
    }
}
