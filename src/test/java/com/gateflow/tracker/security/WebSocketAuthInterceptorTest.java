package com.gateflow.tracker.security;

import com.gateflow.tracker.config.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketAuthInterceptorTest {

    private static final String SECRET = "this-is-a-strong-jwt-secret-key-32+chars!!";
    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 86400000L);
    private final WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(jwtUtil);

    @Test
    void extractTokenParsesQueryString() {
        assertThat(WebSocketAuthInterceptor.extractToken("token=abc&x=1")).isEqualTo("abc");
        assertThat(WebSocketAuthInterceptor.extractToken("x=1&token=abc")).isEqualTo("abc");
        assertThat(WebSocketAuthInterceptor.extractToken("x=1")).isNull();
        assertThat(WebSocketAuthInterceptor.extractToken("")).isNull();
        assertThat(WebSocketAuthInterceptor.extractToken(null)).isNull();
    }

    @Test
    void rejectsHandshakeWithoutToken() {
        ServerHttpRequest req = mock(ServerHttpRequest.class);
        when(req.getURI()).thenReturn(URI.create("ws://host/ws/debug/view/s1"));
        ServerHttpResponse resp = mock(ServerHttpResponse.class);

        boolean ok = interceptor.beforeHandshake(req, resp, null, new HashMap<>());

        assertThat(ok).isFalse();
        verify(resp).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void acceptsHandshakeWithValidToken() {
        String token = jwtUtil.generateToken("alice", "admin");
        ServerHttpRequest req = mock(ServerHttpRequest.class);
        when(req.getURI()).thenReturn(URI.create("ws://host/ws/debug/view/s1?token=" + token));
        ServerHttpResponse resp = mock(ServerHttpResponse.class);
        Map<String, Object> attrs = new HashMap<>();

        boolean ok = interceptor.beforeHandshake(req, resp, null, attrs);

        assertThat(ok).isTrue();
        assertThat(attrs).containsEntry("username", "alice");
    }
}
