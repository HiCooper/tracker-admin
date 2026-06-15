package com.gateflow.tracker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 {@link RequireRole} 注解的 RBAC 拦截器。
 * 读取 {@code JwtAuthFilter} 写入的 request 属性 {@code role} 并校验。
 */
@Component
@RequiredArgsConstructor
public class RbacInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole == null) {
            return true; // 无注解的接口不受限
        }

        Object role = request.getAttribute("role");
        if (role == null) {
            // 正常情况下 JwtAuthFilter 已拦截匿名请求;此处为防御性兜底
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.AUTH_TOKEN_INVALID, "未认证");
            return false;
        }

        boolean allowed = Arrays.asList(requireRole.value()).contains(role.toString());
        if (!allowed) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.AUTH_FORBIDDEN, "无权限执行该操作");
            return false;
        }
        return true;
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
