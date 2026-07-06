package com.study.storage

import com.study.config.MinioProperties
import com.study.exception.InvalidFileException
import io.minio.BucketExistsArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.http.Method
import jakarta.annotation.PostConstruct
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * MinIO 对象存储实现。
 *
 * 本项目只允许上传图片,数据库文章表只保存 object key;详情接口再临时生成预签名 URL,这样 bucket 不需要公开。
 */
// @Service: MinIO 存储实现作为业务 Bean 注册,ArticleService/FileController 依赖的是 StorageService 接口。
@Service
class MinioStorageService(
    private val client: MinioClient,
    properties: MinioProperties,
) : StorageService {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket = properties.bucket
    private val month = DateTimeFormatter.ofPattern("yyyyMM")

    // @PostConstruct: Bean 创建并完成依赖注入后执行一次,适合做本地环境初始化。
    @PostConstruct
    fun ensureBucket() {
        // 本地学习环境首次启动时自动建 bucket,省掉手工进 MinIO Console 初始化。
        try {
            val exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
                log.info("创建 MinIO bucket: {}", bucket)
            }
        } catch (e: Exception) {
            throw IllegalStateException("初始化 MinIO bucket 失败: $bucket", e)
        }
    }

    override fun upload(file: MultipartFile): String {
        // MVP 只做 content-type 白名单;生产环境还应检查魔数、扩展名、大小和病毒扫描。
        val contentType = file.contentType
        if (contentType == null || !contentType.startsWith("image/")) {
            throw InvalidFileException("仅支持图片文件")
        }
        val key = "articles/${LocalDate.now().format(month)}/${UUID.randomUUID()}${extensionOf(file.originalFilename)}"
        return try {
            file.inputStream.use { input ->
                client.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .`object`(key)
                        .stream(input, file.size, -1)
                        .contentType(contentType)
                        .build(),
                )
            }
            key
        } catch (e: Exception) {
            throw IllegalStateException("上传文件失败", e)
        }
    }

    override fun presignedGetUrl(key: String?): String? {
        if (key.isNullOrBlank()) return null
        return try {
            // 预签名 URL 1 小时有效,前端可直接用它显示图片,不用后端转发文件流。
            client.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .`object`(key)
                    .expiry(1, TimeUnit.HOURS)
                    .build(),
            )
        } catch (e: Exception) {
            log.warn("生成预签名 URL 失败 key={}", key, e)
            null
        }
    }

    override fun delete(key: String?) {
        if (key.isNullOrBlank()) return
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(key).build())
        } catch (e: Exception) {
            throw IllegalStateException("删除文件失败", e)
        }
    }

    private fun extensionOf(filename: String?): String {
        if (filename == null) return ""
        val dot = filename.lastIndexOf('.')
        return if (dot >= 0) filename.substring(dot) else ""
    }
}
