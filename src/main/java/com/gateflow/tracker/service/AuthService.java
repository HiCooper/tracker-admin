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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final TrackerUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /** 首次启动初始化 admin 的密码;通过环境变量 ADMIN_INITIAL_PASSWORD 提供,缺省则随机生成。 */
    @Value("${gateflow.admin.initial-password:}")
    private String adminInitialPassword;

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
     * 首次启动且无任何用户时创建初始 admin。
     * 密码来源:环境变量 ADMIN_INITIAL_PASSWORD;未设置则生成一次性随机密码并打印一次。
     * 不再使用众所周知的静态默认密码(admin123),消除可直接登录的安全隐患。
     */
    @PostConstruct
    public void initDefaultAdmin() {
        long count = userMapper.selectCount(new LambdaQueryWrapper<>());
        if (count > 0) {
            return;
        }
        boolean generated = adminInitialPassword == null || adminInitialPassword.isBlank();
        String password = generated ? generateRandomPassword() : adminInitialPassword;

        TrackerUser admin = new TrackerUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole("admin");
        admin.setStatus(1);
        userMapper.insert(admin);

        if (generated) {
            // 仅在未提供 env 时,首启一次性打印随机密码供运维取用,随后应立即修改。
            log.warn("============================================");
            log.warn("  初始 admin 已创建(未设置 ADMIN_INITIAL_PASSWORD)。");
            log.warn("  用户名: admin");
            log.warn("  一次性随机密码: {}", password);
            log.warn("  请立即登录并修改密码。");
            log.warn("============================================");
        } else {
            log.info("初始 admin 已创建,密码取自 ADMIN_INITIAL_PASSWORD,请于首次登录后修改。");
        }
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
