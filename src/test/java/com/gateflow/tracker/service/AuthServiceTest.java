package com.gateflow.tracker.service;

import com.gateflow.tracker.config.JwtUtil;
import com.gateflow.tracker.domain.dto.LoginRequest;
import com.gateflow.tracker.domain.dto.LoginResponse;
import com.gateflow.tracker.domain.entity.TrackerUser;
import com.gateflow.tracker.exception.BizException;
import com.gateflow.tracker.repository.TrackerUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private TrackerUserMapper userMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    private TrackerUser activeUser() {
        TrackerUser u = new TrackerUser();
        u.setUsername("alice");
        u.setPassword("hashed");
        u.setRole("admin");
        u.setStatus(1);
        return u;
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        when(userMapper.selectOne(any())).thenReturn(activeUser());
        when(passwordEncoder.matches("pw", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("alice", "admin")).thenReturn("tok");

        LoginResponse resp = authService.login(loginRequest("alice", "pw"));

        assertThat(resp.getToken()).isEqualTo("tok");
        assertThat(resp.getRole()).isEqualTo("admin");
    }

    @Test
    void loginFailsForUnknownUser() {
        when(userMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> authService.login(loginRequest("ghost", "pw")))
                .isInstanceOf(BizException.class);
    }

    @Test
    void loginFailsForWrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(activeUser());
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);
        assertThatThrownBy(() -> authService.login(loginRequest("alice", "bad")))
                .isInstanceOf(BizException.class);
    }

    @Test
    void initDefaultAdminUsesEnvPasswordNotStaticDefault() {
        ReflectionTestUtils.setField(authService, "adminInitialPassword", "Sup3rSecret!");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");

        authService.initDefaultAdmin();

        // 密码必须取自 env,绝不能是众所周知的 admin123
        verify(passwordEncoder).encode(eq("Sup3rSecret!"));
        ArgumentCaptor<TrackerUser> captor = ArgumentCaptor.forClass(TrackerUser.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
        assertThat(captor.getValue().getRole()).isEqualTo("admin");
        assertThat(captor.getValue().getPassword()).isEqualTo("ENC");
    }

    @Test
    void initDefaultAdminGeneratesRandomWhenNoEnv() {
        ReflectionTestUtils.setField(authService, "adminInitialPassword", "");
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");

        authService.initDefaultAdmin();

        ArgumentCaptor<String> pw = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(pw.capture());
        assertThat(pw.getValue()).isNotBlank().isNotEqualTo("admin123");
        assertThat(pw.getValue().length()).isGreaterThanOrEqualTo(16);
    }

    @Test
    void initDefaultAdminSkipsWhenUsersExist() {
        lenient().when(userMapper.selectCount(any())).thenReturn(3L);
        authService.initDefaultAdmin();
        verify(userMapper, org.mockito.Mockito.never()).insert(any(TrackerUser.class));
    }
}
