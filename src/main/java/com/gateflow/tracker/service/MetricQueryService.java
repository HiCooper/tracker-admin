package com.gateflow.tracker.service;

import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 从 ClickHouse 计算告警所需指标。规则配置 appCode 时按 app 维度过滤(events.app_code),
 * 否则为全局口径。
 */
@Slf4j
@Service
public class MetricQueryService {

    /** 当前窗口与基线值。 */
    public record MetricSample(double current, double baseline) {}

    private final DataSource chDataSource;

    public MetricQueryService(ClickHouseProperties chProps) {
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource for alerting: {}", e.getMessage());
            ds = null;
        }
        this.chDataSource = ds;
    }

    public MetricSample sample(TrackerAlertRule rule) {
        int win = rule.getWindowMinutes() != null ? rule.getWindowMinutes() : 60;
        String app = rule.getAppCode();
        return switch (rule.getMetric()) {
            case "event_volume_drop" -> new MetricSample(currentVolume(win, app), baselineVolume(win, app));
            case "error_rate" -> new MetricSample(errorRate(win, app), 0);
            case "null_rate" -> new MetricSample(nullRate(win, app), 0);
            default -> new MetricSample(0, 0);
        };
    }

    /** 规则配置 appCode 时追加 app 维度过滤(去除单引号防注入,值由管理员配置)。 */
    static String appFilter(String appCode) {
        if (appCode == null || appCode.isBlank()) {
            return "";
        }
        return " AND app_code = '" + appCode.replace("'", "") + "'";
    }

    private double currentVolume(int win, String app) {
        return scalar("SELECT count() FROM gateflow_tracker.events " +
                "WHERE timestamp >= now() - INTERVAL " + win + " MINUTE" + appFilter(app));
    }

    private double baselineVolume(int win, String app) {
        // 同一窗口、前一天
        return scalar("SELECT count() FROM gateflow_tracker.events " +
                "WHERE timestamp >= now() - INTERVAL 1 DAY - INTERVAL " + win + " MINUTE " +
                "AND timestamp < now() - INTERVAL 1 DAY" + appFilter(app));
    }

    private double errorRate(int win, String app) {
        return scalar("SELECT if(count() = 0, 0, countIf(event_type = 'error') / count()) " +
                "FROM gateflow_tracker.events WHERE timestamp >= now() - INTERVAL " + win + " MINUTE" + appFilter(app));
    }

    private double nullRate(int win, String app) {
        return scalar("SELECT if(count() = 0, 0, countIf(user_id = '') / count()) " +
                "FROM gateflow_tracker.events WHERE timestamp >= now() - INTERVAL " + win + " MINUTE" + appFilter(app));
    }

    private double scalar(String sql) {
        if (chDataSource == null) {
            return 0;
        }
        try (Connection conn = chDataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (Exception e) {
            log.error("Alert metric query failed [{}]: {}", sql, e.getMessage());
        }
        return 0;
    }
}
