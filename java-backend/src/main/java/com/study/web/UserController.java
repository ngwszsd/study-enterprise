package com.study.web;

import com.study.service.UserService;
import com.study.web.dto.UserResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户接口:给前端搜索协作成员等轻量场景使用。 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> search(@RequestParam(defaultValue = "") String keyword) {
        return userService.search(keyword);
    }
}
