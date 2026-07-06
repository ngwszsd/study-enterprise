package com.study.config

import io.minio.MinioClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 提供 MinioClient bean。
 *
 * @Configuration 声明配置类;@Bean 把第三方 SDK 客户端交给 Spring 管理,其他服务可直接注入。
 */
@Configuration
class MinioConfig {
    // @Bean 方法参数 MinioProperties 也来自 Spring 容器,不是手动 new。
    @Bean
    fun minioClient(properties: MinioProperties): MinioClient =
        MinioClient.builder()
            .endpoint(properties.endpoint)
            .credentials(properties.accessKey, properties.secretKey)
            .build()
}
