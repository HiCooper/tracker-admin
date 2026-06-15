package com.gateflow.tracker.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注需要特定角色才能访问的接口(方法或类级别)。
 * 由 {@link RbacInterceptor} 在请求进入控制器前校验。
 *
 * <p>此前 JwtAuthFilter 解析了 role claim 但从不校验,任何合法 token 都能删/审任意资源;
 * 本注解 + 拦截器补齐授权环节。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /** 允许访问的角色集合(满足其一即可)。 */
    String[] value();
}
