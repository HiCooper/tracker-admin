package com.gateflow.tracker.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * ShedLock configuration — ensures @Scheduled tasks execute on only one
 * instance in a multi-node deployment.
 *
 * <p>Requires a shedlock table in MySQL (created by Flyway migration):
 * <pre>
 * CREATE TABLE IF NOT EXISTS shedlock (
 *     name       VARCHAR(64)  NOT NULL,
 *     lock_until TIMESTAMP    NOT NULL,
 *     locked_at  TIMESTAMP    NOT NULL,
 *     locked_by  VARCHAR(255) NOT NULL,
 *     PRIMARY KEY (name)
 * );
 * </pre>
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}
