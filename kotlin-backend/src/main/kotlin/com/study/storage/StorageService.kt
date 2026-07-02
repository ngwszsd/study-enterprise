package com.study.storage

import org.springframework.web.multipart.MultipartFile

/** 对象存储抽象,屏蔽 MinIO 细节。 */
interface StorageService {
    /** 上传文件,返回对象 key。 */
    fun upload(file: MultipartFile): String

    /** 按 key 生成预签名 GET URL;失败或 key 为空返回 null。 */
    fun presignedGetUrl(key: String?): String?

    /** 删除对象。 */
    fun delete(key: String?)
}
