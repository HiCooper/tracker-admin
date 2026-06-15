package com.gateflow.tracker.service;

import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.dto.SessionAnalysisVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话分析:直接查询 ClickHouse(结构修正 B)。
 *
 * <p>此前查询的 MySQL {@code tracker_session_agg} 从无写入(空转),分析页恒空。
 * 现实时聚合 {@code gateflow_tracker.sessions}(ReplacingMergeTree,用 FINAL 取已合并行)。
 */
@Slf4j
@Service
public class SessionAnalysisService {

    private static final int DEFAULT_RANGE_DAYS = 30;
    private final DataSource chDataSource;

    public SessionAnalysisService(ClickHouseProperties chProps) {
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource, session analysis will return empty: {}", e.getMessage());
            ds = null;
        }
        this.chDataSource = ds;
    }

    public List<SessionAnalysisVO> query(String sessionId, String userId, LocalDate startDate, LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusDays(DEFAULT_RANGE_DAYS);
        return runQuery(start, end, 1000);
    }

    public List<SessionAnalysisVO> getRecentData(int days) {
        LocalDate end = LocalDate.now();
        return runQuery(end.minusDays(days), end, 100);
    }

    private List<SessionAnalysisVO> runQuery(LocalDate start, LocalDate end, int limit) {
        if (chDataSource == null) {
            return List.of();
        }
        String sql =
                "SELECT toDate(start_time) AS d, toHour(start_time) AS h, platform, " +
                "count() AS session_count, uniqExact(user_id) AS user_count, " +
                "avg(duration) AS avg_duration, avg(page_views) AS avg_page_depth, " +
                "sum(is_bounce) AS bounce_count, " +
                "if(count() = 0, 0, sum(is_bounce) / count()) AS bounce_rate " +
                "FROM gateflow_tracker.sessions FINAL " +
                "WHERE start_time >= ? AND start_time < ? " +
                "GROUP BY d, h, platform ORDER BY d DESC, h DESC LIMIT " + limit;

        List<SessionAnalysisVO> result = new ArrayList<>();
        try (Connection conn = chDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, start.atStartOfDay().toString().replace('T', ' '));
            stmt.setString(2, end.plusDays(1).atStartOfDay().toString().replace('T', ' '));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SessionAnalysisVO vo = new SessionAnalysisVO();
                    vo.setDate(LocalDate.parse(rs.getString("d")));
                    vo.setHour(rs.getInt("h"));
                    vo.setPlatform(rs.getString("platform"));
                    vo.setSessionCount(rs.getLong("session_count"));
                    vo.setUserCount(rs.getLong("user_count"));
                    vo.setAvgDuration(rs.getBigDecimal("avg_duration"));
                    vo.setAvgPageDepth(rs.getBigDecimal("avg_page_depth"));
                    vo.setBounceCount(rs.getLong("bounce_count"));
                    vo.setBounceRate(rs.getBigDecimal("bounce_rate"));
                    result.add(vo);
                }
            }
        } catch (Exception e) {
            log.error("ClickHouse session analysis query failed: {}", e.getMessage(), e);
            return List.of();
        }
        return result;
    }
}
