package com.study.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.study.domain.User
import com.study.exception.ConflictException
import com.study.exception.UnauthorizedException
import com.study.mapper.UserMapper
import com.study.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 认证业务:注册、登陆签发令牌。 */
@Service
class AuthService(
    private val userMapper: UserMapper,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @Transactional
    fun register(username: String, rawPassword: String): User {
        val count = userMapper.selectCount(QueryWrapper<User>().eq("username", username))
        if (count != null && count > 0) {
            throw ConflictException("用户名已存在")
        }
        val user = User().apply {
            this.username = username
            passwordHash = passwordEncoder.encode(rawPassword)
        }
        userMapper.insert(user)
        return user
    }

    /** 校验凭据并返回 JWT;失败统一 401。 */
    @Transactional(readOnly = true)
    fun login(username: String, rawPassword: String): String {
        val user = userMapper.selectOne(QueryWrapper<User>().eq("username", username))
            ?: throw UnauthorizedException("用户名或密码错误")
        if (!passwordEncoder.matches(rawPassword, user.passwordHash)) {
            throw UnauthorizedException("用户名或密码错误")
        }
        return jwtService.generateToken(user.id!!, user.username!!)
    }
}
