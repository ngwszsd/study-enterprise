package com.study.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT 配置(构造器绑定):jwt.secret / jwt.expires-in 。
 *
 * @ConfigurationProperties 把 application.yml / 环境变量里的 jwt 前缀配置绑定到这个 data class。
 */
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val expiresIn: Long,
)
