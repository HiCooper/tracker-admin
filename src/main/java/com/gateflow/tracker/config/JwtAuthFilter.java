package com.gateflow.tracker.config;

import com.gateflow.tracker.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates JWT token on all /api/v1/** requests except auth endpoints.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /** Paths that do NOT require authentication. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/collect"
    );

    /** Path prefixes that do NOT require authentication. */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/swagger",
            "/v3/api-docs",
            "/actuator",
            "/error"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;
        String path = httpReq.getRequestURI();

        // Allow CORS preflight
        if ("OPTIONS".equalsIgnoreCase(httpReq.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Allow public paths
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Validate token
        String header = httpReq.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(httpResp, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.AUTH_TOKEN_INVALID, "未提供有效的认证令牌");
            return;
        }

        String token = header.substring(7);
        if (!jwtUtil.validateToken(token)) {
            writeError(httpResp, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.AUTH_TOKEN_EXPIRED, "认证令牌已过期或无效");
            return;
        }

        // Set user info as request attributes for downstream use
        httpReq.setAttribute("username", jwtUtil.getUsername(token));
        httpReq.setAttribute("role", jwtUtil.getRole(token));

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path)) return true;
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void writeError(HttpServletResponse resp, int status, int code, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        body.put("timestamp", System.currentTimeMillis());
        resp.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
