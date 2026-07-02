package com.study

import com.study.storage.StorageService
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

// 子类固定递归泛型 SELF,避免 Kotlin 下 MySQLContainer<Nothing> 链式调用退化成 Nothing。
private class KMySQLContainer(image: DockerImageName) : MySQLContainer<KMySQLContainer>(image)
private class KGenericContainer(image: DockerImageName) : GenericContainer<KGenericContainer>(image)

/**
 * 集成测试基类:单例 Testcontainers 起 MySQL + Redis(只起一次,JVM 退出由 Ryuk 回收),
 * @DynamicPropertySource 覆盖连接;StorageService 打桩,免依赖 MinIO。
 */
@SpringBootTest
abstract class AbstractIntegrationTest {

    @MockitoBean
    lateinit var storageService: StorageService

    companion object {
        private val MYSQL = KMySQLContainer(DockerImageName.parse("mysql:8.0"))
        private val REDIS = KGenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)

        init {
            MYSQL.start()
            REDIS.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { MYSQL.jdbcUrl }
            registry.add("spring.datasource.username") { MYSQL.username }
            registry.add("spring.datasource.password") { MYSQL.password }
            registry.add("spring.data.redis.host") { REDIS.host }
            registry.add("spring.data.redis.port") { REDIS.getMappedPort(6379) }
        }
    }
}
