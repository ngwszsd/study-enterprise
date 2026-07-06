package com.study.web;

import com.study.security.AuthUser;
import com.study.service.ArticleService;
import com.study.web.dto.ArticleRequest;
import com.study.web.dto.ArticleResponse;
import com.study.web.dto.CategoryCount;
import com.study.web.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章接口:分页列表/详情/新建/更新/删除(/api/articles/**)。均需登陆;改删需作者本人。
 *
 * 【前端类比】对应 frontend/src/api/articles.ts。@AuthenticationPrincipal 拿到的当前用户,就是
 * JWT 过滤器解出来放进去的(前端不用传 userId,服务端从 token 里认)。
 */
// @RestController: 声明这是 REST API 控制器,返回值直接写成 JSON 响应体。
// @RequestMapping: 统一声明文章接口前缀 /api/articles。
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    // @GetMapping: 处理 GET /api/articles;@RequestParam 读取 ?page=0&size=10&keyword=xx。
    @GetMapping
    public PageResponse<ArticleResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return articleService.list(keyword, page, size);
    }

    @GetMapping("/stats")
    public List<CategoryCount> stats() {
        return articleService.stats();
    }

    // @PathVariable: 读取路径里的 {id},例如 GET /api/articles/1。
    @GetMapping("/{id}")
    public ArticleResponse get(@PathVariable Long id) {
        return articleService.get(id);
    }

    // @RequestBody 读取 JSON 正文;@AuthenticationPrincipal 读取当前登录用户。
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse create(@Valid @RequestBody ArticleRequest request,
                                  @AuthenticationPrincipal AuthUser user) {
        return articleService.create(request, user.id());
    }

    @PutMapping("/{id}")
    public ArticleResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ArticleRequest request,
                                  @AuthenticationPrincipal AuthUser user) {
        return articleService.update(id, request, user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        articleService.delete(id, user.id());
    }
}
