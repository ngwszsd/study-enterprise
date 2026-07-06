package com.study.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.domain.User;
import com.study.mapper.UserMapper;
import com.study.web.dto.UserResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户查询业务:只返回可公开展示的用户信息,不暴露密码哈希。 */
@Service
public class UserService {

    private static final int SEARCH_LIMIT = 10;

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> search(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        return userMapper.selectList(new LambdaQueryWrapper<User>()
                        .select(User::getId, User::getUsername)
                        .like(!normalized.isBlank(), User::getUsername, normalized)
                        .orderByAsc(User::getUsername)
                        .last("LIMIT " + SEARCH_LIMIT))
                .stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername()))
                .toList();
    }
}
