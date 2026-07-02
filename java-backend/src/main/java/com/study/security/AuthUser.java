package com.study.security;

/** 认证后的当前用户主体,存入 SecurityContext,可用 @AuthenticationPrincipal 注入。 */
public record AuthUser(Long id, String username) {
}
