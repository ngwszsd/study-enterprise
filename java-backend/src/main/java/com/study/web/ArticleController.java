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

/** 文章接口:分页列表/详情/新建/更新/删除。均需登陆;写操作需作者本人。 */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

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

    @GetMapping("/{id}")
    public ArticleResponse get(@PathVariable Long id) {
        return articleService.get(id);
    }

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
