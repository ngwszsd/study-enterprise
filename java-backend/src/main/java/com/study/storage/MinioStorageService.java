package com.study.storage;

import com.study.config.MinioProperties;
import com.study.exception.InvalidFileException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO 对象存储实现。
 *
 * 本项目只允许上传图片,数据库文章表只保存 object key;详情接口再临时生成预签名 URL,这样 bucket 不需要公开。
 */
// @Service: MinIO 存储实现作为业务 Bean 注册,ArticleService/FileController 依赖的是 StorageService 接口。
@Service
public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final MinioClient client;
    private final String bucket;

    public MinioStorageService(MinioClient client, MinioProperties properties) {
        this.client = client;
        this.bucket = properties.getBucket();
    }

    // @PostConstruct: Bean 创建并完成依赖注入后执行一次,适合做本地环境初始化。
    @PostConstruct
    void ensureBucket() {
        // 本地学习环境首次启动时自动建 bucket,省掉手工进 MinIO Console 初始化。
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("创建 MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            throw new IllegalStateException("初始化 MinIO bucket 失败: " + bucket, e);
        }
    }

    @Override
    public String upload(MultipartFile file) {
        // MVP 只做 content-type 白名单;生产环境还应检查魔数、扩展名、大小和病毒扫描。
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidFileException("仅支持图片文件");
        }
        String key = "articles/" + LocalDate.now().format(MONTH) + "/"
                + UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
            return key;
        } catch (Exception e) {
            throw new IllegalStateException("上传文件失败", e);
        }
    }

    @Override
    public String presignedGetUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            // 预签名 URL 1 小时有效,前端可直接用它显示图片,不用后端转发文件流。
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(key)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            log.warn("生成预签名 URL 失败 key={}", key, e);
            return null;
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception e) {
            throw new IllegalStateException("删除文件失败", e);
        }
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }
}
