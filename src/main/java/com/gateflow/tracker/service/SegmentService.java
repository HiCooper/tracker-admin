package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.domain.entity.TrackerSegment;
import com.gateflow.tracker.repository.TrackerSegmentMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentService {

    private final TrackerSegmentMapper segmentMapper;
    @Qualifier("clickHouseJdbcTemplate")
    private final NamedParameterJdbcTemplate chJdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class SegmentRequest {
        private String segmentName;
        private String segmentKey;
        private String description;
        private Map<String, Object> rules;
        private String refreshInterval;
    }

    @Data
    public static class SegmentEstimate {
        private Long segmentId;
        private String segmentKey;
        private long estimatedSize;
        private String sql;
    }

    public List<TrackerSegment> list() {
        return segmentMapper.selectList(Wrappers.lambdaQuery(TrackerSegment.class)
                .orderByDesc(TrackerSegment::getUpdatedAt));
    }

    public TrackerSegment get(Long id) { return segmentMapper.selectById(id); }

    @Transactional
    public TrackerSegment create(SegmentRequest req) {
        TrackerSegment s = new TrackerSegment();
        s.setSegmentName(req.getSegmentName());
        s.setSegmentKey(req.getSegmentKey());
        s.setDescription(req.getDescription());
        s.setSegmentType("dynamic");
        try { s.setRules(objectMapper.writeValueAsString(req.getRules())); } catch (Exception ignored) {}
        s.setRefreshInterval(req.getRefreshInterval() != null ? req.getRefreshInterval() : "24h");
        s.setStatus(1); s.setCreatedBy("admin");
        segmentMapper.insert(s);
        return s;
    }

    @Transactional
    public TrackerSegment update(Long id, SegmentRequest req) {
        TrackerSegment s = segmentMapper.selectById(id);
        if (s == null) throw new RuntimeException("分群不存在");
        if (req.getSegmentName() != null) s.setSegmentName(req.getSegmentName());
        if (req.getSegmentKey() != null) s.setSegmentKey(req.getSegmentKey());
        if (req.getDescription() != null) s.setDescription(req.getDescription());
        if (req.getRules() != null) {
            try { s.setRules(objectMapper.writeValueAsString(req.getRules())); } catch (Exception ignored) {}
        }
        if (req.getRefreshInterval() != null) s.setRefreshInterval(req.getRefreshInterval());
        segmentMapper.updateById(s);
        return s;
    }

    @Transactional
    public void delete(Long id) { segmentMapper.deleteById(id); }

    @SuppressWarnings("unchecked")
    public SegmentEstimate estimateSize(Long id) {
        TrackerSegment s = segmentMapper.selectById(id);
        if (s == null) throw new RuntimeException("分群不存在");
        SegmentEstimate est = new SegmentEstimate();
        est.setSegmentId(id); est.setSegmentKey(s.getSegmentKey());
        try {
            Map<String, Object> rules = objectMapper.readValue(s.getRules(), new TypeReference<>() {});
            String where = buildWhere(rules);
            String sql = "SELECT count(DISTINCT user_id) AS cnt FROM gateflow_tracker.events WHERE 1=1 " + where;
            est.setSql(sql);
            Map<String, Object> row = chJdbc.queryForMap(sql, new MapSqlParameterSource());
            long cnt = row.get("cnt") != null ? ((Number) row.get("cnt")).longValue() : 0;
            est.setEstimatedSize(cnt);
            s.setEstimatedSize(cnt); s.setLastRefreshedAt(LocalDateTime.now());
            segmentMapper.updateById(s);
        } catch (Exception e) { log.error("Segment estimate failed id={}", id, e); }
        return est;
    }

    @SuppressWarnings("unchecked")
    private String buildWhere(Map<String, Object> node) {
        String op = (String) node.getOrDefault("operator", "and");
        List<Map<String, Object>> conds = (List<Map<String, Object>>) node.get("conditions");
        if (conds == null || conds.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(" AND (");
        for (int i = 0; i < conds.size(); i++) {
            if (i > 0) sb.append(" ").append(op.toUpperCase()).append(" ");
            Map<String, Object> c = conds.get(i);
            String type = (String) c.get("type");
            if ("group".equals(type) && c.containsKey("conditions")) {
                sb.append(buildWhere(c));
            } else if ("event_count".equals(type)) {
                String et = (String) c.get("eventType");
                String cmp = (String) c.get("op");
                Object val = c.get("value");
                sb.append("user_id IN (SELECT user_id FROM gateflow_tracker.events WHERE event_type = '")
                        .append(esc(et)).append("' GROUP BY user_id HAVING count() ")
                        .append(cmpOp(cmp)).append(" ").append(val).append(")");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String cmpOp(String op) {
        return switch (op) { case "gte" -> ">="; case "lte" -> "<="; case "gt" -> ">"; case "lt" -> "<"; default -> "="; };
    }

    private String esc(String s) { return s != null ? s.replace("'", "''") : ""; }
}
