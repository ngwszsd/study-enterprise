package com.study.web;

import com.study.exception.InvalidFileException;
import com.study.storage.StorageService;
import com.study.web.dto.FileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口:上传到 MinIO、按 key 取预签名 URL。均需登陆。
 *
 * @RestController 返回 JSON;@RequestMapping 统一声明 /api/files 前缀。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    // @RequestParam("file"): 读取 multipart/form-data 里的文件字段。
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FileResponse upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("文件为空");
        }
        String key = storageService.upload(file);
        return new FileResponse(key, storageService.presignedGetUrl(key));
    }

    /** key 含斜杠,用查询参数传递:GET /api/files/url?key=articles/202607/xxx.png 。 */
    @GetMapping("/url")
    public FileResponse url(@RequestParam("key") String key) {
        return new FileResponse(key, storageService.presignedGetUrl(key));
    }
}
