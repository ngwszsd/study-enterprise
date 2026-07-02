package com.study.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.study.domain.User;
import com.study.exception.ConflictException;
import com.study.exception.UnauthorizedException;
import com.study.mapper.UserMapper;
import com.study.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** AuthService 单元测试(纯 Mockito,mock UserMapper)。 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserMapper userMapper;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;
    @InjectMocks
    AuthService authService;

    @Test
    void register_success_encodesPasswordAndInserts() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        User user = authService.register("alice", "pw");

        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void register_duplicateUsername_throwsConflict() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register("alice", "pw"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void login_validCredentials_returnsToken() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash("hash");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(jwtService.generateToken(1L, "alice")).thenReturn("tok");

        assertThat(authService.login("alice", "pw")).isEqualTo("tok");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("hash");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice", "bad"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_unknownUser_throwsUnauthorized() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login("ghost", "pw"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
