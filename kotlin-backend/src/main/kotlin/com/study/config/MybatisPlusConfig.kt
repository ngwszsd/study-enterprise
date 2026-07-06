package com.study.config

import com.baomidou.mybatisplus.annotation.DbType
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * MyBatis-Plus 配置:分页插件。
 *
 * @Configuration + @Bean 的组合,就是把分页拦截器注册给 MyBatis-Plus 使用。
 */
@Configuration
class MybatisPlusConfig {
    // @Bean: 返回对象会进入 Spring 容器,MyBatis-Plus 启动时会读取这个拦截器。
    @Bean
    fun mybatisPlusInterceptor(): MybatisPlusInterceptor {
        val interceptor = MybatisPlusInterceptor()
        interceptor.addInnerInterceptor(PaginationInnerInterceptor(DbType.MYSQL))
        return interceptor
    }
}
