package com.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.study.cache.RedisArticleCache;
import com.study.domain.Article;
import com.study.domain.User;
import com.study.exception.ForbiddenException;
import com.study.exception.ResourceNotFoundException;
import com.study.mapper.ArticleMapper;
import com.study.mapper.UserMapper;
import com.study.storage.StorageService;
import com.study.web.dto.ArticleRequest;
import com.study.web.dto.ArticleResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** ArticleService 单元测试:创建、作者归属校验、未找到、删除。 */
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    ArticleMapper articleMapper;
    @Mock
    UserMapper userMapper;
    @Mock
    StorageService storageService;
    @Mock
    RedisArticleCache cache;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @InjectMocks
    ArticleService articleService;

    @Test
    void create_returnsResponseWithAuthorAndZeroViews() {
        when(articleMapper.insert(any(Article.class))).thenReturn(1);
        User author = new User();
        author.setId(1L);
        author.setUsername("alice");
        when(userMapper.selectById(1L)).thenReturn(author);

        ArticleResponse response = articleService.create(new ArticleRequest("t", "c", "cat", null), 1L);

        assertThat(response.title()).isEqualTo("t");
        assertThat(response.authorId()).isEqualTo(1L);
        assertThat(response.authorUsername()).isEqualTo("alice");
        assertThat(response.viewCount()).isEqualTo(0L);
    }

    @Test
    void update_byNonOwner_throwsForbidden() {
        Article article = new Article();
        article.setId(10L);
        article.setAuthorId(1L);
        when(articleMapper.selectById(10L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.update(10L, new ArticleRequest("t2", "c2", null, null), 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void get_missing_throwsNotFound() {
        when(cache.get(99L)).thenReturn(null);
        when(articleMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.get(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_byOwner_deletesAndEvicts() {
        Article article = new Article();
        article.setId(5L);
        article.setAuthorId(1L);
        when(articleMapper.selectById(5L)).thenReturn(article);
        when(articleMapper.deleteById(5L)).thenReturn(1);

        articleService.delete(5L, 1L);

        verify(articleMapper).deleteById(5L);
        verify(cache).evict(5L);
    }
}
