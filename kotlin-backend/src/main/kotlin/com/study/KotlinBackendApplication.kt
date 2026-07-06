package com.study

import org.mybatis.spring.annotation.MapperScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * Kotlin 版后端入口。
 *
 * @SpringBootApplication 是启动入口组合注解:自动配置、组件扫描、注册应用上下文;
 * @ConfigurationPropertiesScan 让 JwtProperties/MinioProperties 等配置绑定类生效;
 * @MapperScan 让 MyBatis-Plus 找到 mapper 接口。
 *
 * 和 Java 版保持同一套 API 契约,但用 Kotlin data class、空安全、构造器注入、apply/let 等写法做对照学习。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.study.mapper")
class KotlinBackendApplication

fun main(args: Array<String>) {
    runApplication<KotlinBackendApplication>(*args)
}
