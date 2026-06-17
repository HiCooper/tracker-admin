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
 * 从 ClickHouse 计算告警所需指标(全局窗口聚合)。
 *
 * <p>注:events 表无 app 维度,当前指标为全局口径(app_code 仅作标签);
 * 按 app 细分需先给 events 增加 app 维度(身份/契约工作项)。
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
        return switch (rule.getMetric()) {
            case "event_volume_drop" -> new MetricSample(currentVolume(win), baselineVolume(win));
            case "error_rate" -> new MetricSample(errorRate(win), 0);
            case "null_rate" -> new MetricSample(nullRate(win), 0);
            default -> new MetricSample(0, 0);
        };
    }

    private double currentVolume(int win) {
        return scalar("SELECT count() FROM gateflow_tracker.events " +
                "WHERE timestamp >= now() - INTERVAL " + win + " MINUTE");
    }

    private double baselineVolume(int win) {
        // 同一窗口、前一天
        return scalar("SELECT count() FROM gateflow_tracker.events " +
                "WHERE timestamp >= now() - INTERVAL 1 DAY - INTERVAL " + win + " MINUTE " +
                "AND timestamp < now() - INTERVAL 1 DAY");
    }

    private double errorRate(int win) {
        return scalar("SELECT if(count() = 0, 0, countIf(event_type = 'error') / count()) " +
                "FROM gateflow_tracker.events WHERE timestamp >= now() - INTERVAL " + win + " MINUTE");
    }

    private double nullRate(int win) {
        return scalar("SELECT if(count() = 0, 0, countIf(user_id = '') / count()) " +
                "FROM gateflow_tracker.events WHERE timestamp >= now() - INTERVAL " + win + " MINUTE");
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
