plugins {
    // 工具链自动解析:声明的 JDK 版本本机没有时,自动从 foojay 下载
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "kotlin-backend"
