package com.study;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Java 版后端入口。
 *
 * @SpringBootApplication 是 Spring Boot 组合注解:自动配置、组件扫描、把当前类作为启动配置入口;
 * @ConfigurationPropertiesScan 让 JwtProperties/MinioProperties 等配置类生效;
 * @MapperScan 让 MyBatis-Plus 找到 mapper 接口。Kotlin 版保持同样业务契约,方便对照语言差异。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan("com.study.mapper")
public class JavaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaBackendApplication.class, args);
    }
}
