package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gateflow.tracker.config.ClickHouseConfig;
import com.gateflow.tracker.config.ClickHouseConfig.ClickHouseProperties;
import com.gateflow.tracker.domain.entity.*;
import com.gateflow.tracker.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class AnalysisService {

    private final DataSource chDataSource;
    private final TrackerAppMapper appMapper;
    private final TrackerPageMapper pageMapper;
    private final TrackerBlockMapper blockMapper;
    private final TrackerFunctionMapper functionMapper;

    public AnalysisService(ClickHouseProperties chProps,
                           TrackerAppMapper appMapper, TrackerPageMapper pageMapper,
                           TrackerBlockMapper blockMapper, TrackerFunctionMapper functionMapper) {
        this.appMapper = appMapper;
        this.pageMapper = pageMapper;
        this.blockMapper = blockMapper;
        this.functionMapper = functionMapper;
        DataSource ds;
        try {
            ds = ClickHouseConfig.createDataSource(chProps);
        } catch (Exception e) {
            log.warn("Failed to create ClickHouse DataSource, analysis will return empty data: {}", e.getMessage());
            ds = null;
        }
        this.chDataSource = ds;
    }

    // ── App Metrics ───────────────────────────────────────

    public List<Map<String, Object>> getAppMetrics(String startTime, String endTime) {
        List<TrackerApp> apps = appMapper.selectList(
                new LambdaQueryWrapper<TrackerApp>().orderByAsc(TrackerApp::getId));
        long startEpoch = toEpochSeconds(startTime);
        long endEpoch = toEpochSeconds(endTime);
        if (endEpoch > 0) endEpoch += 86400;
        Map<String, AppStat> stats = queryAppStats(startEpoch, endEpoch);

        List<Map<String, Object>> result = new ArrayList<>();
        for (TrackerApp a : apps) {
            AppStat s = stats.getOrDefault(a.getAppCode(), AppStat.ZERO);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("appCode", a.getAppCode());
            m.put("appName", a.getAppName());
            m.put("dau", s.dau);
            m.put("totalPv", s.pv);
            m.put("pageCount", pageMapper.selectCount(
                    new LambdaQueryWrapper<TrackerPage>().eq(TrackerPage::getAppId, a.getId())).intValue());
            result.add(m);
        }
        return result;
    }

    // ── Page Metrics ──────────────────────────────────────

    public Map<String, Object> getPageMetrics(String appCode, String startTime, String endTime) {
        TrackerApp app = findAppByCode(appCode);
        List<TrackerPage> pages = pageMapper.selectList(
                new LambdaQueryWrapper<TrackerPage>().eq(TrackerPage::getAppId, app == null ? 0 : app.getId()));
        long startEpoch = toEpochSeconds(startTime);
        long endEpoch = toEpochSeconds(endTime);
        if (endEpoch > 0) endEpoch += 86400; // include full end day
        boolean daily = (endEpoch - startEpoch) > 2 * 86400;
        Map<String, PageStat> stats = queryPageStats(appCode, startEpoch, endEpoch);
        List<Map<String, Object>> trend = queryPageTrend(appCode, startEpoch, endEpoch, daily);

        long totalUv = 0, totalPv = 0;
        List<Map<String, Object>> pageList = new ArrayList<>();
        for (TrackerPage p : pages) {
            PageStat s = stats.getOrDefault(spmSegment(p.getPageCode()), PageStat.ZERO);
            totalPv += s.pv;
            totalUv += s.uv;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pageCode", p.getPageCode());
            m.put("pageName", p.getPageName());
            m.put("pv", s.pv);
            m.put("uv", s.uv);
            m.put("avgStayDuration", s.avgStay);
            m.put("bounceRate", s.bounceRate);
            m.put("blockCount", blockMapper.selectCount(
                    new LambdaQueryWrapper<TrackerBlock>().eq(TrackerBlock::getPageId, p.getId())).intValue());
            pageList.add(m);
        }

        long avgStay = queryPageAvgStay(appCode, startEpoch, endEpoch) / 1000; // ms → s
        long totalSessions = querySessionCount(appCode, startEpoch, endEpoch);
        long bounceCount = queryBounceSessions(appCode, startEpoch, endEpoch);
        double bounceRate = totalSessions > 0 ? Math.round((double) bounceCount / totalSessions * 10000.0) / 10000.0 : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trend", trend);
        result.put("summary", Map.of("totalPv", totalPv, "totalUv", totalUv,
                                      "avgStay", avgStay, "bounceRate", bounceRate));
        result.put("pages", pageList);
        return result;
    }

    // ── Block Metrics ─────────────────────────────────────

    public Map<String, Object> getBlockMetrics(String appCode, String pageCode, String startTime, String endTime) {
        TrackerApp app = findAppByCode(appCode);
        TrackerPage page = app == null ? null : findPageByCode(app.getId(), pageCode);
        List<TrackerBlock> blocks = blockMapper.selectList(
                new LambdaQueryWrapper<TrackerBlock>().eq(TrackerBlock::getPageId, page == null ? 0 : page.getId()));
        long startEpoch = toEpochSeconds(startTime);
        long endEpoch = toEpochSeconds(endTime);
        if (endEpoch > 0) endEpoch += 86400;
        boolean daily = (endEpoch - startEpoch) > 2 * 86400;
        Map<String, BlockStat> stats = queryBlockStats(appCode, pageCode, startEpoch, endEpoch);
        List<Map<String, Object>> trend = queryBlockTrend(appCode, pageCode, startEpoch, endEpoch, daily);

        long totalExpPv = 0, totalExpUv = 0;
        List<Map<String, Object>> blockList = new ArrayList<>();
        for (TrackerBlock b : blocks) {
            BlockStat s = stats.getOrDefault(spmSegment(b.getBlockCode()), BlockStat.ZERO);
            totalExpPv += s.exposurePv;
            totalExpUv += s.exposureUv;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("blockCode", b.getBlockCode());
            m.put("blockName", b.getBlockName());
            m.put("exposurePv", s.exposurePv);
            m.put("exposureUv", s.exposureUv);
            m.put("clickPv", s.clickPv);
            m.put("clickUv", s.clickUv);
            m.put("ctr", s.ctr());
            m.put("functionCount", functionMapper.selectCount(
                    new LambdaQueryWrapper<TrackerFunction>().eq(TrackerFunction::getBlockId, b.getId())).intValue());
            blockList.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trend", trend);
        result.put("summary", Map.of("totalExposurePv", totalExpPv, "totalExposureUv", totalExpUv));
        result.put("blocks", blockList);
        return result;
    }

    // ── Function Metrics ──────────────────────────────────

    public Map<String, Object> getFunctionMetrics(String appCode, String pageCode, String blockCode,
                                                   String startTime, String endTime) {
        TrackerApp app = findAppByCode(appCode);
        TrackerPage page = app == null ? null : findPageByCode(app.getId(), pageCode);
        TrackerBlock block = page == null ? null : findBlockByCode(page.getId(), blockCode);
        List<TrackerFunction> funcs = functionMapper.selectList(
                new LambdaQueryWrapper<TrackerFunction>().eq(TrackerFunction::getBlockId, block == null ? 0 : block.getId()));
        long startEpoch = toEpochSeconds(startTime);
        long endEpoch = toEpochSeconds(endTime);
        if (endEpoch > 0) endEpoch += 86400;
        boolean daily = (endEpoch - startEpoch) > 2 * 86400;
        Map<String, FuncStat> stats = queryFuncStats(appCode, pageCode, blockCode, startEpoch, endEpoch);
        List<Map<String, Object>> trend = queryFuncTrend(appCode, pageCode, blockCode, startEpoch, endEpoch, daily);
        PageStat pageStat = querySinglePageStat(appCode, pageCode, startEpoch, endEpoch);

        long totalExpPv = 0, totalExpUv = 0;
        List<Map<String, Object>> funcList = new ArrayList<>();
        for (TrackerFunction f : funcs) {
            FuncStat s = stats.getOrDefault(spmSegment(f.getFuncCode()), FuncStat.ZERO);
            totalExpPv += s.exposurePv;
            totalExpUv += s.exposureUv;
            double penetration = pageStat.uv > 0 ? (double) s.clickUv / pageStat.uv : 0;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("funcCode", f.getFuncCode());
            m.put("funcName", f.getFuncName());
            m.put("exposurePv", s.exposurePv);
            m.put("exposureUv", s.exposureUv);
            m.put("clickPv", s.clickPv);
            m.put("clickUv", s.clickUv);
            m.put("ctr", s.ctr());
            m.put("penetrationRate", Math.round(penetration * 10000.0) / 10000.0);
            funcList.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trend", trend);
        result.put("summary", Map.of("totalExposurePv", totalExpPv,
                                      "totalExposureUv", totalExpUv,
                                      "functionCount", funcList.size()));
        result.put("functions", funcList);
        return result;
    }

    // ── Trend Detail ──────────────────────────────────────

    public Map<String, Object> getTrendDetail(int days) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        Map<String, DayStat> stats = queryDailyStats(days);

        List<Map<String, Object>> detail = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String key = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
            DayStat s = stats.getOrDefault(key, DayStat.ZERO);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", d.format(fmt));
            day.put("exposurePv", s.exposurePv);
            day.put("exposureUv", s.exposureUv);
            day.put("clickPv", s.clickPv);
            day.put("clickUv", s.clickUv);
            day.put("ctr", s.ctr());
            day.put("penetrationRate", 0.0);
            detail.add(day);
        }
        return Map.of("detail", detail);
    }

    // ── ClickHouse Queries ────────────────────────────────

    private Connection getChConnection() throws SQLException {
        if (chDataSource == null) throw new SQLException("ClickHouse DataSource not available");
        return chDataSource.getConnection();
    }

    private Map<String, AppStat> queryAppStats(long startEpoch, long endEpoch) {
        boolean hasTime = startEpoch > 0 || endEpoch > 0;
        String sql = (hasTime ? """
            SELECT spma,
                   countIf(event_type = 'page_view') as pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'page_view') as dau
            FROM gateflow_tracker.events
            WHERE spma != ''
              AND timestamp >= ? AND timestamp <= ?
            GROUP BY spma
            """ : """
            SELECT spma,
                   countIf(event_type = 'page_view') as pv,
                   uniqIf(user_id, event_type = 'page_view') as dau
            FROM gateflow_tracker.events
            WHERE spma != ''
            GROUP BY spma
            """);
        Map<String, AppStat> map = new LinkedHashMap<>();
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            if (hasTime) { st.setLong(1, startEpoch); st.setLong(2, endEpoch); }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("spma");
                    map.put(code, new AppStat(rs.getLong("pv"), rs.getLong("dau")));
                }
            }
        } catch (SQLException e) {
            log.warn("ClickHouse app stats query failed: {}", e.getMessage());
        }
        return map;
    }

    private Map<String, PageStat> queryPageStats(String appCode, long startEpoch, long endEpoch) {
        boolean hasTime = startEpoch > 0 || endEpoch > 0;
        String sql = (hasTime ? """
            SELECT spmb,
                   countIf(event_type = 'page_view') as pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'page_view') as uv,
                   avgIf(stay_duration, event_type = 'page_view') as avg_stay
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb != ''
              AND timestamp >= ? AND timestamp <= ?
            GROUP BY spmb
            """ : """
            SELECT spmb,
                   countIf(event_type = 'page_view') as pv,
                   uniqIf(user_id, event_type = 'page_view') as uv,
                   avgIf(stay_duration, event_type = 'page_view') as avg_stay
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb != ''
            GROUP BY spmb
            """);
        Map<String, PageStat> map = new LinkedHashMap<>();
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            if (hasTime) { st.setLong(2, startEpoch); st.setLong(3, endEpoch); }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("spmb");
                    long uv = rs.getLong("uv");
                    map.put(code, new PageStat(rs.getLong("pv"), uv,
                            (long) rs.getDouble("avg_stay"), uv > 0 ? 0.0 : 0.0));
                }
            }
        } catch (SQLException e) {
            log.warn("ClickHouse page stats query failed: {}", e.getMessage());
        }
        return map;
    }

    private PageStat querySinglePageStat(String appCode, String pageCode, long startEpoch, long endEpoch) {
        boolean hasTime = startEpoch > 0 || endEpoch > 0;
        String sql = (hasTime ? """
            SELECT uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'page_view') as uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ?
              AND timestamp >= ? AND timestamp <= ?
            """ : """
            SELECT uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'page_view') as uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ?
            """);
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            st.setString(2, spmSegment(pageCode));
            if (hasTime) { st.setLong(3, startEpoch); st.setLong(4, endEpoch); }
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return new PageStat(0, rs.getLong("uv"), 0, 0);
            }
        } catch (SQLException e) {
            log.warn("ClickHouse single page query failed: {}", e.getMessage());
        }
        return PageStat.ZERO;
    }

    private Map<String, BlockStat> queryBlockStats(String appCode, String pageCode, long startEpoch, long endEpoch) {
        boolean hasTime = startEpoch > 0 || endEpoch > 0;
        String sql = (hasTime ? """
            SELECT spmc,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'exposure') as exp_uv,
                   countIf(event_type = 'click') as click_pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'click') as click_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc != ''
              AND timestamp >= ? AND timestamp <= ?
            GROUP BY spmc
            """ : """
            SELECT spmc,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv,
                   countIf(event_type = 'click') as click_pv,
                   uniqIf(user_id, event_type = 'click') as click_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc != ''
            GROUP BY spmc
            """);
        Map<String, BlockStat> map = new LinkedHashMap<>();
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            st.setString(2, spmSegment(pageCode));
            if (hasTime) { st.setLong(3, startEpoch); st.setLong(4, endEpoch); }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("spmc");
                    map.put(code, new BlockStat(rs.getLong("exp_pv"), rs.getLong("exp_uv"),
                            rs.getLong("click_pv"), rs.getLong("click_uv")));
                }
            }
        } catch (SQLException e) {
            log.warn("ClickHouse block stats query failed: {}", e.getMessage());
        }
        return map;
    }

    private Map<String, FuncStat> queryFuncStats(String appCode, String pageCode, String blockCode, long startEpoch, long endEpoch) {
        boolean hasTime = startEpoch > 0 || endEpoch > 0;
        String sql = (hasTime ? """
            SELECT spmd,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'exposure') as exp_uv,
                   countIf(event_type = 'click') as click_pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'click') as click_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc = ? AND spmd != ''
              AND timestamp >= ? AND timestamp <= ?
            GROUP BY spmd
            """ : """
            SELECT spmd,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv,
                   countIf(event_type = 'click') as click_pv,
                   uniqIf(user_id, event_type = 'click') as click_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc = ? AND spmd != ''
            GROUP BY spmd
            """);
        Map<String, FuncStat> map = new LinkedHashMap<>();
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            st.setString(2, spmSegment(pageCode));
            st.setString(3, spmSegment(blockCode));
            if (hasTime) { st.setLong(4, startEpoch); st.setLong(5, endEpoch); }
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("spmd");
                    map.put(code, new FuncStat(rs.getLong("exp_pv"), rs.getLong("exp_uv"),
                            rs.getLong("click_pv"), rs.getLong("click_uv")));
                }
            }
        } catch (SQLException e) {
            log.warn("ClickHouse function stats query failed: {}", e.getMessage());
        }
        return map;
    }

    private List<Map<String, Object>> queryPageTrend(String appCode, long startEpoch, long endEpoch, boolean daily) {
        String sql = daily ? """
            SELECT toDate(toDateTime(timestamp)) as hr,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND timestamp >= ? AND timestamp <= ?
            GROUP BY hr ORDER BY hr
            """ : """
            SELECT toStartOfHour(toDateTime(timestamp)) as hr,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND timestamp >= ? AND timestamp <= ?
            GROUP BY hr ORDER BY hr
            """;
        return executeTrendQuery(sql, daily, appCode, startEpoch, endEpoch);
    }

    private List<Map<String, Object>> queryBlockTrend(String appCode, String pageCode, long startEpoch, long endEpoch, boolean daily) {
        String sql = daily ? """
            SELECT toDate(toDateTime(timestamp)) as hr,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND timestamp >= ? AND timestamp <= ?
            GROUP BY hr ORDER BY hr
            """ : """
            SELECT toStartOfHour(toDateTime(timestamp)) as hr,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND timestamp >= ? AND timestamp <= ?
            GROUP BY hr ORDER BY hr
            """;
        return executeTrendQuery(sql, daily, appCode, spmSegment(pageCode), startEpoch, endEpoch);
    }

    private List<Map<String, Object>> queryFuncTrend(String appCode, String pageCode, String blockCode, long startEpoch, long endEpoch, boolean daily) {
        String sql = daily ? """
            SELECT toDate(toDateTime(timestamp)) as hr,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc = ? AND timestamp >= ? AND timestamp <= ?
            GROUP BY hr ORDER BY hr
            """ : """
            SELECT toStartOfHour(toDateTime(timestamp)) as hr,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc = ? AND timestamp >= ? AND timestamp <= ?
            GROUP BY hr ORDER BY hr
            """;
        return executeTrendQuery(sql, daily, appCode, spmSegment(pageCode), spmSegment(blockCode), startEpoch, endEpoch);
    }

    private Map<String, DayStat> queryDailyStats(int days) {
        String sql = """
            SELECT toDate(toDateTime(timestamp)) as d,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'exposure') as exp_uv,
                   countIf(event_type = 'click') as click_pv,
                   uniqIf(if(user_id != '', user_id, anonymous_id), event_type = 'click') as click_uv
            FROM gateflow_tracker.events
            WHERE toDateTime(timestamp) >= now() - INTERVAL ? DAY
            GROUP BY d ORDER BY d
            """;
        Map<String, DayStat> map = new LinkedHashMap<>();
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setInt(1, days);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String date = rs.getString("d");
                    map.put(date, new DayStat(rs.getLong("exp_pv"), rs.getLong("exp_uv"),
                            rs.getLong("click_pv"), rs.getLong("click_uv")));
                }
            }
        } catch (SQLException e) {
            log.warn("ClickHouse daily stats query failed: {}", e.getMessage());
        }
        return map;
    }

    private List<Map<String, Object>> executeTrendQuery(String sql, boolean daily, Object... params) {
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Long l) st.setLong(i + 1, l);
                else st.setString(i + 1, (String) params[i]);
            }
            try (ResultSet rs = st.executeQuery()) {
                return mapTrendResult(rs, daily);
            }
        } catch (SQLException e) {
            log.warn("ClickHouse trend query failed: {}", e.getMessage());
        }
        return emptyTrend(daily);
    }

    private List<Map<String, Object>> mapTrendResult(ResultSet rs, boolean daily) throws SQLException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(daily ? "MM-dd" : "HH:mm");
        List<Map<String, Object>> trend = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("time", rs.getTimestamp("hr").toLocalDateTime().format(fmt));
            p.put("exposurePv", rs.getLong("exp_pv"));
            p.put("exposureUv", rs.getLong("exp_uv"));
            trend.add(p);
        }
        return trend;
    }

    private List<Map<String, Object>> emptyTrend(boolean daily) {
        List<Map<String, Object>> trend = new ArrayList<>();
        if (daily) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
            LocalDate today = LocalDate.now();
            for (int d = 6; d >= 0; d--) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("time", today.minusDays(d).format(fmt));
                p.put("exposurePv", 0);
                p.put("exposureUv", 0);
                trend.add(p);
            }
        } else {
            for (int h = 0; h < 24; h++) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("time", String.format("%02d:00", h));
                p.put("exposurePv", 0);
                p.put("exposureUv", 0);
                trend.add(p);
            }
        }
        return trend;
    }

    /**
     * Get the exposure PV+UV for a specific block.
     * Used as CTR denominator for functions, and for the summary cards.
     */
    private BlockStat queryBlockExposure(String appCode, String pageCode, String blockCode, long startEpoch, long endEpoch) {
        boolean hasTime = startEpoch > 0 || endEpoch > 0;
        String sql = (hasTime ? """
            SELECT countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc = ?
              AND timestamp >= ? AND timestamp <= ?
            """ : """
            SELECT countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc = ?
            """);
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            st.setString(2, spmSegment(pageCode));
            st.setString(3, spmSegment(blockCode));
            if (hasTime) { st.setLong(4, startEpoch); st.setLong(5, endEpoch); }
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return new BlockStat(rs.getLong("exp_pv"), rs.getLong("exp_uv"), 0, 0);
            }
        } catch (SQLException e) {
            log.warn("ClickHouse block exposure query failed: {}", e.getMessage());
        }
        return new BlockStat(1, 1, 0, 0); // avoid division by zero
    }

    private long queryPageAvgStay(String appCode, long startEpoch, long endEpoch) {
        boolean hasTime = startEpoch > 0 || endEpoch > 0;
        String sql = (hasTime ? """
            SELECT avg(stay_duration) as avg_stay
            FROM gateflow_tracker.events
            WHERE spma = ? AND event_type = 'stay'
              AND timestamp >= ? AND timestamp <= ?
            """ : """
            SELECT avg(stay_duration) as avg_stay
            FROM gateflow_tracker.events
            WHERE spma = ? AND event_type = 'stay'
            """);
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            if (hasTime) { st.setLong(2, startEpoch); st.setLong(3, endEpoch); }
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) { double v = rs.getDouble("avg_stay"); if (!rs.wasNull()) return (long) v; }
            }
        } catch (SQLException e) {
            log.warn("ClickHouse avg stay query failed: {}", e.getMessage());
        }
        return 0;
    }

    private long querySessionCount(String appCode, long startEpoch, long endEpoch) {
        boolean hasTime = startEpoch > 0 || endEpoch > 0;
        String sql = (hasTime ? """
            SELECT uniq(session_id) as cnt
            FROM gateflow_tracker.events
            WHERE spma = ? AND timestamp >= ? AND timestamp <= ?
            """ : """
            SELECT uniq(session_id) as cnt
            FROM gateflow_tracker.events
            WHERE spma = ?
            """);
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            if (hasTime) { st.setLong(2, startEpoch); st.setLong(3, endEpoch); }
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getLong("cnt");
            }
        } catch (SQLException e) {
            log.warn("ClickHouse session count query failed: {}", e.getMessage());
        }
        return 1; // avoid div-by-zero
    }

    private long queryBounceSessions(String appCode, long startEpoch, long endEpoch) {
        // Bounce = sessions with exactly 1 page_view
        boolean hasTime = startEpoch > 0 || endEpoch > 0;
        String sql = (hasTime ? """
            SELECT countIf(pv = 1) as bounce
            FROM (
              SELECT session_id, countIf(event_type = 'page_view') as pv
              FROM gateflow_tracker.events
              WHERE spma = ? AND timestamp >= ? AND timestamp <= ?
              GROUP BY session_id
            )
            """ : """
            SELECT countIf(pv = 1) as bounce
            FROM (
              SELECT session_id, countIf(event_type = 'page_view') as pv
              FROM gateflow_tracker.events
              WHERE spma = ?
              GROUP BY session_id
            )
            """);
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            if (hasTime) { st.setLong(2, startEpoch); st.setLong(3, endEpoch); }
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return rs.getLong("bounce");
            }
        } catch (SQLException e) {
            log.warn("ClickHouse bounce query failed: {}", e.getMessage());
        }
        return 0;
    }

    // ── Helpers ────────────────────────────────────────────

    private TrackerApp findAppByCode(String appCode) {
        return appMapper.selectOne(
                new LambdaQueryWrapper<TrackerApp>().eq(TrackerApp::getAppCode, appCode));
    }

    private TrackerPage findPageByCode(Long appId, String pageCode) {
        return pageMapper.selectOne(new LambdaQueryWrapper<TrackerPage>()
                .eq(TrackerPage::getAppId, appId).eq(TrackerPage::getPageCode, pageCode));
    }

    private TrackerBlock findBlockByCode(Long pageId, String blockCode) {
        return blockMapper.selectOne(new LambdaQueryWrapper<TrackerBlock>()
                .eq(TrackerBlock::getPageId, pageId).eq(TrackerBlock::getBlockCode, blockCode));
    }

    // ── SPM Helpers ─────────────────────────────────────────

    /**
     * Extract the last segment of an SPM code for ClickHouse matching.
     * e.g. "a_a_policy_report.b_homepage" → "b_homepage"
     *      "a_a_policy_report.b_homepage.c_top_banner" → "c_top_banner"
     *      "a_policy_report" → "a_policy_report" (no dot, return as-is)
     */
    private String spmSegment(String fullCode) {
        int lastDot = fullCode.lastIndexOf('.');
        return lastDot >= 0 ? fullCode.substring(lastDot + 1) : fullCode;
    }

    /**
     * Convert a date string ("2026-06-14" or "2026-06-14T00:00:00") to epoch seconds.
     * Returns 0 for null/empty (treated as unbounded).
     */
    private long toEpochSeconds(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return 0;
        String date = dateStr.contains("T") ? dateStr.substring(0, dateStr.indexOf('T')) : dateStr;
        return LocalDate.parse(date).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
    }

    // ── Stat Records ───────────────────────────────────────

    record AppStat(long pv, long dau) { static final AppStat ZERO = new AppStat(0, 0); }
    record PageStat(long pv, long uv, long avgStay, double bounceRate) { static final PageStat ZERO = new PageStat(0, 0, 0, 0); }
    record BlockStat(long exposurePv, long exposureUv, long clickPv, long clickUv) {
        static final BlockStat ZERO = new BlockStat(0, 0, 0, 0);
        double ctr() { return exposurePv > 0 ? Math.round((double) clickPv / exposurePv * 10000.0) / 10000.0 : 0; }
    }
    record FuncStat(long exposurePv, long exposureUv, long clickPv, long clickUv) {
        static final FuncStat ZERO = new FuncStat(0, 0, 0, 0);
        double ctr() { return exposurePv > 0 ? Math.round((double) clickPv / exposurePv * 10000.0) / 10000.0 : 0; }
    }
    record DayStat(long exposurePv, long exposureUv, long clickPv, long clickUv) {
        static final DayStat ZERO = new DayStat(0, 0, 0, 0);
        double ctr() { return exposurePv > 0 ? Math.round((double) clickPv / exposurePv * 10000.0) / 10000.0 : 0; }
    }
}
