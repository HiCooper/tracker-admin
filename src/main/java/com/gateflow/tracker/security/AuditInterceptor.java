package com.gateflow.tracker.security;

import com.gateflow.tracker.domain.entity.TrackerAuditLog;
import com.gateflow.tracker.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 记录所有变更类(POST/PUT/DELETE/PATCH)管理操作到审计日志:谁、何时、对什么路径、结果状态。
 * 审计在 afterCompletion 进行,失败不影响主流程(由 AuditLogService 兜底)。
 */
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING = Set.of("POST", "PUT", "DELETE", "PATCH");
    /** 不审计的路径前缀:鉴权与采集端点(非管理操作)。 */
    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            "/api/v1/auth", "/api/v1/collect");
    private static final String START_ATTR = "auditStartMs";

    private final AuditLogService auditLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        if (!isAuditable(request)) {
            return;
        }
        Long start = (Long) request.getAttribute(START_ATTR);
        long duration = start != null ? System.currentTimeMillis() - start : 0L;
        Object username = request.getAttribute("username");
        Object role = request.getAttribute("role");

        TrackerAuditLog entry = TrackerAuditLog.builder()
                .username(username != null ? username.toString() : "anonymous")
                .role(role != null ? role.toString() : null)
                .method(request.getMethod())
                .path(request.getRequestURI())
                .status(response.getStatus())
                .requestId(MDC.get("requestId"))
                .ip(clientIp(request))
                .durationMs(duration)
                .build();
        auditLogService.record(entry);
    }

    private boolean isAuditable(HttpServletRequest request) {
        if (!MUTATING.contains(request.getMethod().toUpperCase())) {
            return false;
        }
        String path = request.getRequestURI();
        return EXCLUDED_PREFIXES.stream().noneMatch(path::startsWith);
    }

    static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
