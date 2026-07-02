package com.study

import org.junit.jupiter.api.Test

/** 上下文加载冒烟;数据源与 Redis 由 Testcontainers 提供(见 AbstractIntegrationTest)。 */
class KotlinBackendApplicationTests : AbstractIntegrationTest() {

    @Test
    fun contextLoads() {
    }
}
