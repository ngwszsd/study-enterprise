package com.study.storage;

import org.springframework.web.multipart.MultipartFile;

/** 对象存储抽象,屏蔽 MinIO 细节(将来可换实现)。 */
public interface StorageService {

    /** 上传文件,返回对象 key。 */
    String upload(MultipartFile file);

    /** 按 key 生成预签名 GET URL;失败或 key 为空返回 null。 */
    String presignedGetUrl(String key);

    /** 删除对象。 */
    void delete(String key);
}
