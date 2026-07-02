package com.study.security

/** 认证后的当前用户主体,存入 SecurityContext,可用 @AuthenticationPrincipal 注入。 */
data class AuthUser(val id: Long, val username: String)
