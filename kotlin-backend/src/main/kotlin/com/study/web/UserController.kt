package com.study.web

import com.study.service.UserService
import com.study.web.dto.UserResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 用户接口:给前端搜索协作成员等轻量场景使用。 */
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @GetMapping
    fun search(@RequestParam(defaultValue = "") keyword: String?): List<UserResponse> = userService.search(keyword)
}
