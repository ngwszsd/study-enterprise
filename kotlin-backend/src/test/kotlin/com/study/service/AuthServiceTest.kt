package com.study.service

import com.baomidou.mybatisplus.core.conditions.Wrapper
import com.study.domain.User
import com.study.exception.ConflictException
import com.study.exception.UnauthorizedException
import com.study.mapper.UserMapper
import com.study.security.JwtService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

/** AuthService 单元测试(MockK,mock UserMapper)。 */
class AuthServiceTest {

    private val userMapper = mockk<UserMapper>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val jwtService = mockk<JwtService>()
    private val authService = AuthService(userMapper, passwordEncoder, jwtService)

    @Test
    fun `注册成功 加密并插入`() {
        every { userMapper.selectCount(any<Wrapper<User>>()) } returns 0L
        every { passwordEncoder.encode("pw") } returns "hash"
        every { userMapper.insert(any<User>()) } returns 1

        val user = authService.register("alice", "pw")

        assertEquals("alice", user.username)
        assertEquals("hash", user.passwordHash)
    }

    @Test
    fun `用户名重复 抛冲突`() {
        every { userMapper.selectCount(any<Wrapper<User>>()) } returns 1L

        assertThrows(ConflictException::class.java) { authService.register("alice", "pw") }
    }

    @Test
    fun `登陆成功 返回令牌`() {
        val user = User().apply {
            id = 1L
            username = "alice"
            passwordHash = "hash"
        }
        every { userMapper.selectOne(any<Wrapper<User>>()) } returns user
        every { passwordEncoder.matches("pw", "hash") } returns true
        every { jwtService.generateToken(1L, "alice") } returns "tok"

        assertEquals("tok", authService.login("alice", "pw"))
    }

    @Test
    fun `密码错误 抛未授权`() {
        val user = User().apply {
            username = "alice"
            passwordHash = "hash"
        }
        every { userMapper.selectOne(any<Wrapper<User>>()) } returns user
        every { passwordEncoder.matches("bad", "hash") } returns false

        assertThrows(UnauthorizedException::class.java) { authService.login("alice", "bad") }
    }

    @Test
    fun `用户不存在 抛未授权`() {
        every { userMapper.selectOne(any<Wrapper<User>>()) } returns null

        assertThrows(UnauthorizedException::class.java) { authService.login("ghost", "pw") }
    }
}
