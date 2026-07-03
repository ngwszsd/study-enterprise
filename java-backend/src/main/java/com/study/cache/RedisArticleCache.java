package com.study.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.domain.Article;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 用 Redis 做两件事(手动序列化,完全可控):
 * 1) 文章详情缓存(读穿透 + 10 分钟 TTL,写时失效);
 * 2) 文章浏览量计数(INCR)。
 * 注入 Boot 提供的 ObjectMapper(已含 JavaTimeModule,LocalDateTime 走 ISO 字符串)。
 *
 * 【前端类比】像 react-query / SWR 的缓存:先看缓存有没有,没有再查库并写回,数据变了就让缓存失效。
 * key 带应用名前缀(java-backend: / kotlin-backend:),避免两套后端共用一个 Redis 时撞键。
 */
@Component
public class RedisArticleCache {

    private static final Logger log = LoggerFactory.getLogger(RedisArticleCache.class);
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;

    public RedisArticleCache(StringRedisTemplate redis, ObjectMapper objectMapper,
                             @Value("${spring.application.name}") String appName) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.keyPrefix = appName + ":"; // 按应用命名空间隔离,避免多后端共用 Redis 撞键
    }

    /** 命中返回文章,未命中或反序列化失败返回 null。 */
    public Article get(Long id) {
        String json = redis.opsForValue().get(detailKey(id));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Article.class);
        } catch (JsonProcessingException e) {
            log.warn("缓存反序列化失败 id={},按未命中处理", id, e);
            redis.delete(detailKey(id));
            return null;
        }
    }

    public void put(Article article) {
        try {
            redis.opsForValue().set(detailKey(article.getId()),
                    objectMapper.writeValueAsString(article), TTL);
        } catch (JsonProcessingException e) {
            log.warn("缓存写入失败 id={},跳过缓存", article.getId(), e);
        }
    }

    public void evict(Long id) {
        redis.delete(detailKey(id));
    }

    /** 浏览量 +1 并返回最新值。 */
    public long incrementViews(Long id) {
        Long v = redis.opsForValue().increment(viewsKey(id));
        return v == null ? 0L : v;
    }

    /** 只读取浏览量,不自增。 */
    public long getViews(Long id) {
        String v = redis.opsForValue().get(viewsKey(id));
        return v == null ? 0L : Long.parseLong(v);
    }

    private String detailKey(Long id) {
        return keyPrefix + "article:" + id;
    }

    private String viewsKey(Long id) {
        return keyPrefix + "article:views:" + id;
    }
}
