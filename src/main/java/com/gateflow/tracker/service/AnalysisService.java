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
        Map<String, AppStat> stats = queryAppStats();

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
        Map<String, PageStat> stats = queryPageStats(appCode);
        List<Map<String, Object>> trend = queryPageTrend(appCode);

        long totalUv = 0, totalPv = 0;
        List<Map<String, Object>> pageList = new ArrayList<>();
        for (TrackerPage p : pages) {
            PageStat s = stats.getOrDefault(p.getPageCode(), PageStat.ZERO);
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

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trend", trend);
        result.put("summary", Map.of("totalPv", totalPv, "totalUv", totalUv));
        result.put("pages", pageList);
        return result;
    }

    // ── Block Metrics ─────────────────────────────────────

    public Map<String, Object> getBlockMetrics(String appCode, String pageCode, String startTime, String endTime) {
        TrackerApp app = findAppByCode(appCode);
        TrackerPage page = app == null ? null : findPageByCode(app.getId(), pageCode);
        List<TrackerBlock> blocks = blockMapper.selectList(
                new LambdaQueryWrapper<TrackerBlock>().eq(TrackerBlock::getPageId, page == null ? 0 : page.getId()));
        Map<String, BlockStat> stats = queryBlockStats(appCode, pageCode);
        List<Map<String, Object>> trend = queryBlockTrend(appCode, pageCode);

        long totalExpPv = 0, totalExpUv = 0;
        List<Map<String, Object>> blockList = new ArrayList<>();
        for (TrackerBlock b : blocks) {
            BlockStat s = stats.getOrDefault(b.getBlockCode(), BlockStat.ZERO);
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
        Map<String, FuncStat> stats = queryFuncStats(appCode, pageCode, blockCode);
        List<Map<String, Object>> trend = queryFuncTrend(appCode, pageCode, blockCode);
        PageStat pageStat = querySinglePageStat(appCode, pageCode);

        long totalExpPv = 0, totalExpUv = 0;
        List<Map<String, Object>> funcList = new ArrayList<>();
        for (TrackerFunction f : funcs) {
            FuncStat s = stats.getOrDefault(f.getFuncCode(), FuncStat.ZERO);
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
        result.put("summary", Map.of("totalExposurePv", totalExpPv, "totalExposureUv", totalExpUv));
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

    private Map<String, AppStat> queryAppStats() {
        String sql = """
            SELECT spma,
                   countIf(event_type = 'page_view') as pv,
                   uniqIf(user_id, event_type = 'page_view') as dau
            FROM gateflow_tracker.events
            WHERE spma != ''
            GROUP BY spma
            """;
        Map<String, AppStat> map = new LinkedHashMap<>();
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                String code = rs.getString("spma");
                map.put(code, new AppStat(rs.getLong("pv"), rs.getLong("dau")));
            }
        } catch (SQLException e) {
            log.warn("ClickHouse app stats query failed: {}", e.getMessage());
        }
        return map;
    }

    private Map<String, PageStat> queryPageStats(String appCode) {
        String sql = """
            SELECT spmb,
                   countIf(event_type = 'page_view') as pv,
                   uniqIf(user_id, event_type = 'page_view') as uv,
                   avgIf(stay_duration, event_type = 'page_view') as avg_stay
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb != ''
            GROUP BY spmb
            """;
        Map<String, PageStat> map = new LinkedHashMap<>();
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("spmb");
                    long uv = rs.getLong("uv");
                    // bounce rate simplified: 1 page-view sessions / total
                    map.put(code, new PageStat(rs.getLong("pv"), uv,
                            (long) rs.getDouble("avg_stay"), uv > 0 ? 0.0 : 0.0));
                }
            }
        } catch (SQLException e) {
            log.warn("ClickHouse page stats query failed: {}", e.getMessage());
        }
        return map;
    }

    private PageStat querySinglePageStat(String appCode, String pageCode) {
        String sql = """
            SELECT uniqIf(user_id, event_type = 'page_view') as uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ?
            """;
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            st.setString(2, pageCode);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return new PageStat(0, rs.getLong("uv"), 0, 0);
            }
        } catch (SQLException e) {
            log.warn("ClickHouse single page query failed: {}", e.getMessage());
        }
        return PageStat.ZERO;
    }

    private Map<String, BlockStat> queryBlockStats(String appCode, String pageCode) {
        String sql = """
            SELECT spmc,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv,
                   countIf(event_type = 'click') as click_pv,
                   uniqIf(user_id, event_type = 'click') as click_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc != ''
            GROUP BY spmc
            """;
        Map<String, BlockStat> map = new LinkedHashMap<>();
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            st.setString(2, pageCode);
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

    private Map<String, FuncStat> queryFuncStats(String appCode, String pageCode, String blockCode) {
        String sql = """
            SELECT spmd,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv,
                   countIf(event_type = 'click') as click_pv,
                   uniqIf(user_id, event_type = 'click') as click_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc = ? AND spmd != ''
            GROUP BY spmd
            """;
        Map<String, FuncStat> map = new LinkedHashMap<>();
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            st.setString(1, appCode);
            st.setString(2, pageCode);
            st.setString(3, blockCode);
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

    private List<Map<String, Object>> queryPageTrend(String appCode) {
        String sql = """
            SELECT toStartOfHour(timestamp) as hr,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND timestamp >= now() - INTERVAL 24 HOUR
            GROUP BY hr ORDER BY hr
            """;
        return executeTrendQuery(sql, appCode);
    }

    private List<Map<String, Object>> queryBlockTrend(String appCode, String pageCode) {
        String sql = """
            SELECT toStartOfHour(timestamp) as hr,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND timestamp >= now() - INTERVAL 24 HOUR
            GROUP BY hr ORDER BY hr
            """;
        String[] params = {appCode, pageCode};
        return executeTrendQuery(sql, params);
    }

    private List<Map<String, Object>> queryFuncTrend(String appCode, String pageCode, String blockCode) {
        String sql = """
            SELECT toStartOfHour(timestamp) as hr,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv
            FROM gateflow_tracker.events
            WHERE spma = ? AND spmb = ? AND spmc = ? AND timestamp >= now() - INTERVAL 24 HOUR
            GROUP BY hr ORDER BY hr
            """;
        String[] params = {appCode, pageCode, blockCode};
        return executeTrendQuery(sql, params);
    }

    private Map<String, DayStat> queryDailyStats(int days) {
        String sql = """
            SELECT toDate(timestamp) as d,
                   countIf(event_type = 'exposure') as exp_pv,
                   uniqIf(user_id, event_type = 'exposure') as exp_uv,
                   countIf(event_type = 'click') as click_pv,
                   uniqIf(user_id, event_type = 'click') as click_uv
            FROM gateflow_tracker.events
            WHERE timestamp >= now() - INTERVAL ? DAY
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

    private List<Map<String, Object>> executeTrendQuery(String sql, String param) {
        return executeTrendQuery(sql, new String[]{param});
    }

    private List<Map<String, Object>> executeTrendQuery(String sql, String[] params) {
        try (Connection c = getChConnection();
             PreparedStatement st = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) st.setString(i + 1, params[i]);
            try (ResultSet rs = st.executeQuery()) {
                return mapTrendResult(rs);
            }
        } catch (SQLException e) {
            log.warn("ClickHouse trend query failed: {}", e.getMessage());
        }
        return emptyTrend();
    }

    private List<Map<String, Object>> mapTrendResult(ResultSet rs) throws SQLException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
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

    private List<Map<String, Object>> emptyTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        for (int h = 0; h < 24; h++) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("time", String.format("%02d:00", h));
            p.put("exposurePv", 0);
            p.put("exposureUv", 0);
            trend.add(p);
        }
        return trend;
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
