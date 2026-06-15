package com.gateflow.tracker.security;

import com.gateflow.tracker.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 调试 WebSocket(观看端 /ws/debug/view/*)握手鉴权。
 *
 * <p>浏览器 WebSocket 无法自定义 Header,故从查询参数 {@code ?token=<JWT>} 取令牌校验。
 * 校验通过才放行,杜绝此前「任何人可连任意调试会话偷看」的问题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request.getURI().getQuery());
        if (token == null || !jwtUtil.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("WebSocket 握手被拒:缺少有效 token, uri={}", request.getURI().getPath());
            return false;
        }
        attributes.put("username", jwtUtil.getUsername(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    /** 从 query string 中提取 token 参数。 */
    static String extractToken(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && "token".equals(pair.substring(0, eq))) {
                String value = pair.substring(eq + 1);
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }
}
