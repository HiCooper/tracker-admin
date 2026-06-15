package com.gateflow.tracker.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 密钥强制策略与令牌签发/解析的单元测试。
 */
class JwtUtilTest {

    private static final String STRONG_SECRET = "this-is-a-strong-jwt-secret-key-32+chars!!";

    @Test
    void blankSecretFailsFast() {
        assertThatThrownBy(() -> new JwtUtil("", 86400000L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtUtil("too-short", 86400000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void generatesAndParsesToken() {
        JwtUtil jwt = new JwtUtil(STRONG_SECRET, 86400000L);
        String token = jwt.generateToken("alice", "admin");

        assertThat(jwt.validateToken(token)).isTrue();
        assertThat(jwt.getUsername(token)).isEqualTo("alice");
        assertThat(jwt.getRole(token)).isEqualTo("admin");

        Claims claims = jwt.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo("alice");
    }

    @Test
    void rejectsTamperedToken() {
        JwtUtil jwt = new JwtUtil(STRONG_SECRET, 86400000L);
        assertThat(jwt.validateToken("not-a-real-token")).isFalse();
    }

    @Test
    void tokenFromDifferentSecretIsInvalid() {
        JwtUtil signer = new JwtUtil(STRONG_SECRET, 86400000L);
        JwtUtil other = new JwtUtil("a-completely-different-secret-key-32chars!", 86400000L);
        String token = signer.generateToken("bob", "admin");
        assertThat(other.validateToken(token)).isFalse();
    }
}
