package com.study.web

import com.study.exception.InvalidFileException
import com.study.storage.StorageService
import com.study.web.dto.FileResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/** 文件接口:上传到 MinIO、按 key 取预签名 URL。均需登陆。 */
@RestController
@RequestMapping("/api/files")
class FileController(private val storageService: StorageService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(@RequestParam("file") file: MultipartFile): FileResponse {
        if (file.isEmpty) {
            throw InvalidFileException("文件为空")
        }
        val key = storageService.upload(file)
        return FileResponse(key, storageService.presignedGetUrl(key))
    }

    /** key 含斜杠,用查询参数:GET /api/files/url?key=articles/202607/xxx.png 。 */
    @GetMapping("/url")
    fun url(@RequestParam("key") key: String): FileResponse = FileResponse(key, storageService.presignedGetUrl(key))
}
