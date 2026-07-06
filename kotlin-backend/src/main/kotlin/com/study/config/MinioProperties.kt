package com.study.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * MinIO 配置(构造器绑定):minio.* 。
 *
 * @ConfigurationProperties 把 minio.endpoint/access-key/secret-key/bucket 绑定到这个 data class。
 */
@ConfigurationProperties(prefix = "minio")
data class MinioProperties(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
)
