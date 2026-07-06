package com.study.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.study.domain.User
import com.study.mapper.UserMapper
import com.study.web.dto.UserResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 用户查询业务:只返回可公开展示的用户信息,不暴露密码哈希。
 *
 * @Service 把它注册成业务 Bean,由 UserController 构造器注入。
 */
@Service
class UserService(
    private val userMapper: UserMapper,
) {
    companion object {
        private const val SEARCH_LIMIT = 10
    }

    // @Transactional(readOnly = true): 搜索用户只读,不需要写库事务。
    @Transactional(readOnly = true)
    fun search(keyword: String?): List<UserResponse> {
        val normalized = keyword?.trim().orEmpty()
        // 只查 id/username 两列,成员搜索不需要 password_hash 等敏感字段。
        val query = QueryWrapper<User>()
            .select("id", "username")
            .like(normalized.isNotBlank(), "username", normalized)
            .orderByAsc("username")
            .last("LIMIT $SEARCH_LIMIT")
        return userMapper.selectList(query).map { UserResponse(it.id, it.username) }
    }
}
