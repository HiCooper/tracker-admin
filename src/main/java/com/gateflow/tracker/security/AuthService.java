package com.gateflow.tracker.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public record LoginResult(String token, String username, String role) {}

    public LoginResult login(String username, String password) {
        var list = jdbcTemplate.queryForList(
                "SELECT username, password, role FROM tracker_user WHERE username = ? AND enabled = 1 AND deleted = 0",
                username);

        if (list.isEmpty()) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        Map<String, Object> user = list.get(0);
        String encodedPassword = (String) user.get("password");

        if (!passwordEncoder.matches(password, encodedPassword)) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String role = (String) user.get("role");
        String token = jwtUtil.generateToken(username, role);
        return new LoginResult(token, username, role);
    }

    public void ensureDefaultAdmin() {
        var count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tracker_user WHERE username = 'admin' AND deleted = 0",
                Integer.class);
        if (count != null && count == 0) {
            String encoded = passwordEncoder.encode("admin123");
            jdbcTemplate.update(
                    "INSERT INTO tracker_user (username, password, role) VALUES (?, ?, ?)",
                    "admin", encoded, "ADMIN");
            log.info("Default admin user created: admin/admin123");
        }
    }
}
