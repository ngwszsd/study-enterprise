package com.study.config

import org.springframework.boot.context.properties.ConfigurationProperties

/** MinIO 配置(构造器绑定):minio.* 。 */
@ConfigurationProperties(prefix = "minio")
data class MinioProperties(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
)
