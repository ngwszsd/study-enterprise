package com.study.web

import com.study.service.UserService
import com.study.web.dto.UserResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 用户接口:给前端搜索协作成员等轻量场景使用。
 *
 * 当前只暴露公开字段 id/username,协作成员面板用它避免用户手动查数据库找 userId。
 */
// @RestController + @RequestMapping: 声明 /api/users 下的 JSON API。
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    // @GetMapping: 处理 GET /api/users;@RequestParam 读取 keyword 查询条件。
    @GetMapping
    fun search(@RequestParam(defaultValue = "") keyword: String?): List<UserResponse> = userService.search(keyword)
}
