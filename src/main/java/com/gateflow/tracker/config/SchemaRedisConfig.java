package com.gateflow.tracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 仅为「发布事件契约到 Redis」提供最小化 Redis 模板。
 *
 * <p>本服务默认排除了 Redis 自动配置(见 application.yml),这里手动定义非池化 Lettuce 工厂
 * (惰性连接,Redis 不可用不影响启动)与 StringRedisTemplate。
 */
@Configuration
public class SchemaRedisConfig {

    @Bean
    public LettuceConnectionFactory schemaRedisConnectionFactory(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password) {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            cfg.setPassword(password);
        }
        return new LettuceConnectionFactory(cfg);
    }

    @Bean
    public StringRedisTemplate schemaStringRedisTemplate(RedisConnectionFactory schemaRedisConnectionFactory) {
        return new StringRedisTemplate(schemaRedisConnectionFactory);
    }
}
