package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.config.JwtUtil;
import com.gateflow.tracker.domain.dto.LoginRequest;
import com.gateflow.tracker.domain.dto.LoginResponse;
import com.gateflow.tracker.domain.entity.TrackerUser;
import com.gateflow.tracker.exception.BizException;
import com.gateflow.tracker.exception.ErrorCode;
import com.gateflow.tracker.repository.TrackerUserMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TrackerUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        TrackerUser user = userMapper.selectOne(
                new LambdaQueryWrapper<TrackerUser>()
                        .eq(TrackerUser::getUsername, request.getUsername())
        );
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException(ErrorCode.AUTH_ACCOUNT_DISABLED, "账户已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.AUTH_BAD_CREDENTIALS, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        log.info("User '{}' logged in successfully", user.getUsername());
        return new LoginResponse(token, user.getRole());
    }

    /**
     * Create default admin user on first startup if none exists.
     * Default credentials: admin / admin123
     */
    @PostConstruct
    public void initDefaultAdmin() {
        long count = userMapper.selectCount(new LambdaQueryWrapper<>());
        if (count == 0) {
            TrackerUser admin = new TrackerUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("admin");
            admin.setStatus(1);
            userMapper.insert(admin);
            log.info("============================================");
            log.info("  Default admin user created.");
            log.info("  Username: admin");
            log.info("  Password: admin123");
            log.info("  Please change the password immediately.");
            log.info("============================================");
        }
    }
}
