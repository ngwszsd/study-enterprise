plugins {
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
}

group = "com.study"
version = "0.0.1-SNAPSHOT"
description = "企业级全链路学习后端(Kotlin)"

repositories {
    mavenCentral()
}

val mybatisPlusVersion = "3.5.12"
val jjwtVersion = "0.12.6"
val minioVersion = "8.5.17"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // MyBatis-Plus + 分页 jsqlparser
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:$mybatisPlusVersion")
    implementation("com.baomidou:mybatis-plus-jsqlparser:$mybatisPlusVersion")

    // 迁移 + 驱动
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")

    // 对象存储
    implementation("io.minio:minio:$minioVersion")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // 测试(Gradle 读 Gradle 元数据,mockk 直接用聚合坐标即可,无需 mockk-jvm)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:mysql")
    testImplementation("io.mockk:mockk:1.14.11")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    // 与 Java 侧统一在 Java 21:用 JDK 21 工具链编译+测试(本机无则 foojay 自动下载),
    // 不受机器 JAVA_HOME 影响。
    jvmToolchain(21)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
