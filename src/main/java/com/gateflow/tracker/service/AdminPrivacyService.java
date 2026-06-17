package com.gateflow.tracker.service;

import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * 隐私合规——被遗忘权:删除某用户在 ClickHouse 中的所有事件与会话数据。
 *
 * <p>通过 ClickHouse 异步 mutation({@code ALTER TABLE ... DELETE WHERE user_id = ?})执行。
 * 删除请求由 AuditInterceptor 自动记入审计日志(DELETE 端点)。
 */
@Slf4j
@Service
public class AdminPrivacyService {

    private static final String EVENTS = "gateflow_tracker.events";
    private static final String SESSIONS = "gateflow_tracker.sessions";

    private final DataSource chDataSource;

    public AdminPrivacyService(ClickHouseProperties chProps) {
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource for privacy ops: {}", e.getMessage());
            ds = null;
        }
        this.chDataSource = ds;
    }

    /**
     * 删除用户数据(被遗忘权)。返回是否成功提交删除。
     */
    public boolean deleteUserData(String userId) {
        if (!StringUtils.hasText(userId)) {
            return false;
        }
        if (chDataSource == null) {
            log.warn("ClickHouse unavailable, cannot delete user data for {}", userId);
            return false;
        }
        try {
            runDelete(chDataSource, EVENTS, userId);
            runDelete(chDataSource, SESSIONS, userId);
            log.info("Submitted privacy deletion for user {}", userId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete user data for {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /** 对单表提交按 user_id 的删除 mutation。 */
    static void runDelete(DataSource ds, String table, String userId) throws Exception {
        String sql = "ALTER TABLE " + table + " DELETE WHERE user_id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.executeUpdate();
        }
    }
}
