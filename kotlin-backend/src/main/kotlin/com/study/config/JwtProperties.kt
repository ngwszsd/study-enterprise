package com.study.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** JWT 配置(构造器绑定):jwt.secret / jwt.expires-in 。 */
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val expiresIn: Long,
)
