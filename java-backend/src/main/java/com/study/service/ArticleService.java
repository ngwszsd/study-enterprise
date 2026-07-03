package com.study.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.cache.RedisArticleCache;
import com.study.domain.Article;
import com.study.domain.User;
import com.study.event.ArticleCreatedEvent;
import com.study.exception.ForbiddenException;
import com.study.exception.ResourceNotFoundException;
import com.study.mapper.ArticleMapper;
import com.study.mapper.UserMapper;
import com.study.storage.StorageService;
import com.study.web.dto.ArticleRequest;
import com.study.web.dto.ArticleResponse;
import com.study.web.dto.CategoryCount;
import com.study.web.dto.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 文章业务:CRUD + 分页搜索 + 作者归属校验 + Redis 缓存/浏览量。 */
@Service
public class ArticleService {

    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final StorageService storageService;
    private final RedisArticleCache cache;
    private final ApplicationEventPublisher eventPublisher;

    public ArticleService(ArticleMapper articleMapper, UserMapper userMapper,
                          StorageService storageService, RedisArticleCache cache,
                          ApplicationEventPublisher eventPublisher) {
        this.articleMapper = articleMapper;
        this.userMapper = userMapper;
        this.storageService = storageService;
        this.cache = cache;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<ArticleResponse> list(String keyword, int page, int size) {
        // MyBatis-Plus 分页 current 从 1 开始;对外仍用 0 基页码
        Page<Article> mpPage = new Page<>(page + 1, size);
        LambdaQueryWrapper<Article> query = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            query.like(Article::getTitle, keyword.trim());
        }
        query.orderByDesc(Article::getCreatedAt);
        articleMapper.selectPage(mpPage, query);

        List<Long> authorIds = mpPage.getRecords().stream().map(Article::getAuthorId).distinct().toList();
        Map<Long, String> usernames = authorIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(authorIds).stream()
                        .collect(Collectors.toMap(User::getId, User::getUsername));

        List<ArticleResponse> items = mpPage.getRecords().stream()
                .map(a -> toResponse(a, usernames.get(a.getAuthorId()), cache.getViews(a.getId())))
                .toList();
        return new PageResponse<>(items, page, size, mpPage.getTotal(), (int) mpPage.getPages());
    }

    @Transactional(readOnly = true)
    public ArticleResponse get(Long id) {
        Article article = cache.get(id);
        if (article == null) {
            article = articleMapper.selectById(id);
            if (article == null) {
                throw new ResourceNotFoundException("文章不存在");
            }
            cache.put(article);
        }
        long views = cache.incrementViews(id);
        return toResponse(article, usernameOf(article.getAuthorId()), views);
    }

    @Transactional
    public ArticleResponse create(ArticleRequest request, Long authorId) {
        Article article = new Article();
        article.setTitle(request.title());
        article.setContent(request.content());
        article.setCategory(request.category());
        article.setCoverImageKey(request.coverImageKey());
        article.setAuthorId(authorId);
        articleMapper.insert(article);
        String author = usernameOf(authorId);
        eventPublisher.publishEvent(new ArticleCreatedEvent(article.getId(), article.getTitle(), author));
        return toResponse(article, author, 0L);
    }

    @Transactional
    public ArticleResponse update(Long id, ArticleRequest request, Long currentUserId) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new ResourceNotFoundException("文章不存在");
        }
        ensureOwner(article, currentUserId);
        article.setTitle(request.title());
        article.setContent(request.content());
        article.setCategory(request.category());
        article.setCoverImageKey(request.coverImageKey());
        articleMapper.updateById(article);
        cache.evict(id);
        return toResponse(article, usernameOf(article.getAuthorId()), cache.getViews(id));
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new ResourceNotFoundException("文章不存在");
        }
        ensureOwner(article, currentUserId);
        articleMapper.deleteById(id);
        cache.evict(id);
        if (article.getCoverImageKey() != null) {
            storageService.delete(article.getCoverImageKey()); // 删文章时清理 MinIO 封面对象
        }
    }

    /** 按分类统计文章数(手写 SQL)。 */
    @Transactional(readOnly = true)
    public List<CategoryCount> stats() {
        return articleMapper.countByCategory();
    }

    private void ensureOwner(Article article, Long userId) {
        if (!article.getAuthorId().equals(userId)) {
            throw new ForbiddenException("只能操作自己的文章");
        }
    }

    private String usernameOf(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getUsername();
    }

    private ArticleResponse toResponse(Article a, String authorUsername, long viewCount) {
        String coverUrl = a.getCoverImageKey() == null
                ? null
                : storageService.presignedGetUrl(a.getCoverImageKey());
        return new ArticleResponse(
                a.getId(), a.getTitle(), a.getContent(), a.getCategory(),
                a.getCoverImageKey(), coverUrl, a.getAuthorId(), authorUsername,
                a.getCreatedAt(), a.getUpdatedAt(), viewCount);
    }
}
