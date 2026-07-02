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

/** MinIO 对象存储实现。 */
@Service
class MinioStorageService(
    private val client: MinioClient,
    properties: MinioProperties,
) : StorageService {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket = properties.bucket
    private val month = DateTimeFormatter.ofPattern("yyyyMM")

    @PostConstruct
    fun ensureBucket() {
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
