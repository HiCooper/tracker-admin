package com.gateflow.tracker.config;

import com.gateflow.tracker.security.RbacInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 RBAC 拦截器,对 {@code /api/v1/**} 下带 {@link com.gateflow.tracker.security.RequireRole}
 * 注解的接口做角色校验。
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RbacInterceptor rbacInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rbacInterceptor).addPathPatterns("/api/v1/**");
    }
}
