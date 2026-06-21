package com.gateflow.tracker.service;

import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.dto.EventAnalysisQueryRequest;
import com.gateflow.tracker.domain.dto.EventAnalysisVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 事件分析:直接查询 ClickHouse 明细聚合(结构修正 B)。
 *
 * <p>此前查询的 MySQL {@code tracker_event_agg} 从无 @Scheduled 写入(空转),导致分析页恒空。
 * 现统一以 ClickHouse 为分析存储,实时聚合 {@code gateflow_tracker.events}。
 */
@Slf4j
@Service
public class EventAnalysisService {

    private static final int DEFAULT_RANGE_DAYS = 30;
    private final DataSource chDataSource;

    public EventAnalysisService(ClickHouseProperties chProps) {
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource, event analysis will return empty: {}", e.getMessage());
            ds = null;
        }
        this.chDataSource = ds;
    }

    public List<EventAnalysisVO> query(EventAnalysisQueryRequest request) {
        LocalDate end = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();
        LocalDate start = request.getStartDate() != null ? request.getStartDate() : end.minusDays(DEFAULT_RANGE_DAYS);
        return runQuery(start, end, request.getEventKey(), request.getPlatform(), 1000);
    }

    public List<EventAnalysisVO> getRecentData(int days) {
        LocalDate end = LocalDate.now();
        return runQuery(end.minusDays(days), end, null, null, 100);
    }

    private List<EventAnalysisVO> runQuery(LocalDate start, LocalDate end,
                                           String eventKey, String platform, int limit) {
        if (chDataSource == null) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder(
                "SELECT toDate(timestamp) AS d, toHour(timestamp) AS h, platform, event_type, " +
                "count() AS event_count, uniqExact(if(user_id != '', user_id, anonymous_id)) AS user_count, uniqExact(anonymous_id) AS device_count " +
                "FROM gateflow_tracker.events " +
                "WHERE timestamp >= ? AND timestamp < ? ");
        boolean hasEventKey = StringUtils.hasText(eventKey);
        boolean hasPlatform = StringUtils.hasText(platform);
        if (hasEventKey) sql.append("AND event_type LIKE ? ");
        if (hasPlatform) sql.append("AND platform = ? ");
        sql.append("GROUP BY d, h, platform, event_type ORDER BY d DESC, h DESC LIMIT ").append(limit);

        List<EventAnalysisVO> result = new ArrayList<>();
        try (Connection conn = chDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int i = 1;
            stmt.setString(i++, start.atStartOfDay().toString().replace('T', ' '));
            stmt.setString(i++, end.plusDays(1).atStartOfDay().toString().replace('T', ' '));
            if (hasEventKey) stmt.setString(i++, "%" + eventKey + "%");
            if (hasPlatform) stmt.setString(i++, platform);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    EventAnalysisVO vo = new EventAnalysisVO();
                    vo.setDate(LocalDate.parse(rs.getString("d")));
                    vo.setHour(rs.getInt("h"));
                    vo.setPlatform(rs.getString("platform"));
                    vo.setEventType(rs.getString("event_type"));
                    vo.setEventCount(rs.getLong("event_count"));
                    vo.setUserCount(rs.getLong("user_count"));
                    vo.setDeviceCount(rs.getLong("device_count"));
                    result.add(vo);
                }
            }
        } catch (Exception e) {
            log.error("ClickHouse event analysis query failed: {}", e.getMessage(), e);
            return List.of();
        }
        return result;
    }
}
