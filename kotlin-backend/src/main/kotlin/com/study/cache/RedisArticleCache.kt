package com.study.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.study.domain.Article
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * 用 Redis 做两件事(手动序列化,完全可控):
 * 1) 文章详情缓存(读穿透 + 10 分钟 TTL,写时失效);
 * 2) 文章浏览量计数(INCR)。
 *
 * 【前端类比】像 react-query / SWR 的缓存:先看缓存有没有,没有再查库并写回,数据变了就让缓存失效。
 * key 带应用名前缀(java-backend: / kotlin-backend:),避免两套后端共用一个 Redis 时撞键。
 */
// @Component: 通用组件 Bean。这里不是典型业务 Service,所以用 Component 更贴合职责。
@Component
class RedisArticleCache(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    // @Value: 注入 spring.application.name,用于拼 Redis key 命名空间。
    @Value("\${spring.application.name}") appName: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val ttl: Duration = Duration.ofMinutes(10)
    private val keyPrefix = "$appName:" // 按应用命名空间隔离,避免多后端共用 Redis 撞键

    fun get(id: Long): Article? {
        val json = redis.opsForValue().get(detailKey(id)) ?: return null
        return try {
            objectMapper.readValue(json, Article::class.java)
        } catch (e: Exception) {
            log.warn("缓存反序列化失败 id={},按未命中处理", id, e)
            redis.delete(detailKey(id))
            null
        }
    }

    fun put(article: Article) {
        try {
            redis.opsForValue().set(detailKey(article.id!!), objectMapper.writeValueAsString(article), ttl)
        } catch (e: Exception) {
            log.warn("缓存写入失败 id={},跳过缓存", article.id, e)
        }
    }

    fun evict(id: Long) {
        redis.delete(detailKey(id))
    }

    /** 浏览量 +1 并返回最新值。 */
    fun incrementViews(id: Long): Long = redis.opsForValue().increment(viewsKey(id)) ?: 0L

    /** 只读浏览量。 */
    fun getViews(id: Long): Long = redis.opsForValue().get(viewsKey(id))?.toLong() ?: 0L

    private fun detailKey(id: Long) = "${keyPrefix}article:$id"
    private fun viewsKey(id: Long) = "${keyPrefix}article:views:$id"
}
