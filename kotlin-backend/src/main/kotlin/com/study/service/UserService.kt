package com.study.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.study.domain.User
import com.study.mapper.UserMapper
import com.study.web.dto.UserResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 用户查询业务:只返回可公开展示的用户信息,不暴露密码哈希。 */
@Service
class UserService(
    private val userMapper: UserMapper,
) {
    companion object {
        private const val SEARCH_LIMIT = 10
    }

    @Transactional(readOnly = true)
    fun search(keyword: String?): List<UserResponse> {
        val normalized = keyword?.trim().orEmpty()
        val query = QueryWrapper<User>()
            .select("id", "username")
            .like(normalized.isNotBlank(), "username", normalized)
            .orderByAsc("username")
            .last("LIMIT $SEARCH_LIMIT")
        return userMapper.selectList(query).map { UserResponse(it.id, it.username) }
    }
}
