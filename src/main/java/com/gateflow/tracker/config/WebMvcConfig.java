package com.gateflow.tracker.config;

import com.gateflow.tracker.security.AuditInterceptor;
import com.gateflow.tracker.security.RbacInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册拦截器:
 * - RBAC:对 {@code /api/v1/**} 下带 {@link com.gateflow.tracker.security.RequireRole} 的接口做角色校验。
 * - 审计:记录 {@code /api/v1/**} 下的变更类操作。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RbacInterceptor rbacInterceptor;
    private final AuditInterceptor auditInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rbacInterceptor).addPathPatterns("/api/v1/**");
        registry.addInterceptor(auditInterceptor).addPathPatterns("/api/v1/**");
    }
}
