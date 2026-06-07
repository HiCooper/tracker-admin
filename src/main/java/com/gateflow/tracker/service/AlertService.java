package com.gateflow.tracker.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateflow.tracker.domain.entity.TrackerAlertRecord;
import com.gateflow.tracker.domain.entity.TrackerAlertRule;
import com.gateflow.tracker.repository.TrackerAlertRecordMapper;
import com.gateflow.tracker.repository.TrackerAlertRuleMapper;
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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final TrackerAlertRuleMapper ruleMapper;
    private final TrackerAlertRecordMapper recordMapper;

    @Qualifier("clickHouseJdbcTemplate")
    private final NamedParameterJdbcTemplate chJdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Data
    public static class AlertRuleRequest {
        private String ruleName;
        private String metric;
        private String eventType;
        private String aggregation;
        private Map<String, String> dimensionFilters;
        private String conditionType;
        private Double threshold;
        private String comparisonPeriod;
        private String checkInterval;
        private String channels;
        private String webhookUrl;
    }

    @Data
    public static class AlertCheckResult {
        private Long ruleId;
        private String ruleName;
        private boolean triggered;
        private String message;
        private Double currentValue;
        private Double thresholdValue;
    }

    public List<TrackerAlertRule> listRules() {
        return ruleMapper.selectList(Wrappers.lambdaQuery(TrackerAlertRule.class)
                .orderByDesc(TrackerAlertRule::getCreatedAt));
    }

    public TrackerAlertRule getRule(Long id) {
        return ruleMapper.selectById(id);
    }

    @Transactional
    public TrackerAlertRule createRule(AlertRuleRequest req) {
        TrackerAlertRule r = new TrackerAlertRule();
        r.setRuleName(req.getRuleName());
        r.setMetric(req.getMetric());
        r.setEventType(req.getEventType());
        r.setAggregation(req.getAggregation() != null ? req.getAggregation() : "count");
        if (req.getDimensionFilters() != null) {
            try { r.setDimensionFilters(objectMapper.writeValueAsString(req.getDimensionFilters())); }
            catch (Exception ignored) {}
        }
        r.setConditionType(req.getConditionType());
        r.setThreshold(req.getThreshold());
        r.setComparisonPeriod(req.getComparisonPeriod() != null ? req.getComparisonPeriod() : "1h");
        r.setCheckInterval(req.getCheckInterval() != null ? req.getCheckInterval() : "1h");
        r.setChannels(req.getChannels() != null ? req.getChannels() : "console");
        r.setWebhookUrl(req.getWebhookUrl());
        r.setStatus(1);
        ruleMapper.insert(r);
        return r;
    }

    @Transactional
    public TrackerAlertRule updateRule(Long id, AlertRuleRequest req) {
        TrackerAlertRule r = ruleMapper.selectById(id);
        if (r == null) throw new RuntimeException("告警规则不存在");
        if (req.getRuleName() != null) r.setRuleName(req.getRuleName());
        if (req.getMetric() != null) r.setMetric(req.getMetric());
        if (req.getEventType() != null) r.setEventType(req.getEventType());
        if (req.getAggregation() != null) r.setAggregation(req.getAggregation());
        if (req.getDimensionFilters() != null) {
            try { r.setDimensionFilters(objectMapper.writeValueAsString(req.getDimensionFilters())); }
            catch (Exception ignored) {}
        }
        if (req.getConditionType() != null) r.setConditionType(req.getConditionType());
        if (req.getThreshold() != null) r.setThreshold(req.getThreshold());
        if (req.getComparisonPeriod() != null) r.setComparisonPeriod(req.getComparisonPeriod());
        if (req.getCheckInterval() != null) r.setCheckInterval(req.getCheckInterval());
        if (req.getChannels() != null) r.setChannels(req.getChannels());
        if (req.getWebhookUrl() != null) r.setWebhookUrl(req.getWebhookUrl());
        ruleMapper.updateById(r);
        return r;
    }

    @Transactional
    public void deleteRule(Long id) { ruleMapper.deleteById(id); }

    @Transactional
    public void toggleRule(Long id, boolean enable) {
        TrackerAlertRule r = ruleMapper.selectById(id);
        if (r == null) throw new RuntimeException("告警规则不存在");
        r.setStatus(enable ? 1 : 0);
        ruleMapper.updateById(r);
    }

    public List<AlertCheckResult> checkAll() {
        List<TrackerAlertRule> rules = ruleMapper.selectList(
                Wrappers.lambdaQuery(TrackerAlertRule.class).eq(TrackerAlertRule::getStatus, 1));
        return rules.stream().map(this::checkRule).collect(Collectors.toList());
    }

    public AlertCheckResult checkRule(TrackerAlertRule rule) {
        AlertCheckResult result = new AlertCheckResult();
        result.setRuleId(rule.getId());
        result.setRuleName(rule.getRuleName());
        result.setThresholdValue(rule.getThreshold());
        result.setTriggered(false);
        try {
            double cv = queryCurrent(rule);
            result.setCurrentValue(cv);
            double th = rule.getThreshold() != null ? rule.getThreshold() : 0;
            boolean trig = false;
            String msg = "";
            switch (rule.getConditionType()) {
                case "above_threshold" -> { trig = cv > th; msg = String.format("当前值 %.2f 超过阈值 %.2f", cv, th); }
                case "below_threshold" -> { trig = cv < th; msg = String.format("当前值 %.2f 低于阈值 %.2f", cv, th); }
                case "decrease_pct" -> {
                    double pv = queryPrevious(rule);
                    double pct = pv > 0 ? (pv - cv) / pv : 0;
                    trig = pct > th / 100;
                    msg = String.format("当前值 %.2f 较上期下降 %.1f%%", cv, pct * 100);
                }
                case "increase_pct" -> {
                    double pv = queryPrevious(rule);
                    double pct = pv > 0 ? (cv - pv) / pv : 0;
                    trig = pct > th / 100;
                    msg = String.format("当前值 %.2f 较上期上升 %.1f%%", cv, pct * 100);
                }
            }
            result.setTriggered(trig);
            result.setMessage(msg);
            rule.setLastCheckedAt(LocalDateTime.now());
            if (trig) {
                rule.setLastTriggeredAt(LocalDateTime.now());
                TrackerAlertRecord rec = new TrackerAlertRecord();
                rec.setRuleId(rule.getId()); rec.setRuleName(rule.getRuleName());
                rec.setMetric(rule.getMetric()); rec.setCurrentValue(cv);
                rec.setThresholdValue(th); rec.setAlertMessage(msg);
                rec.setTriggeredAt(LocalDateTime.now());
                recordMapper.insert(rec);
                log.warn("ALERT: rule={} msg={}", rule.getRuleName(), msg);
            }
            ruleMapper.updateById(rule);
        } catch (Exception e) {
            log.error("Alert check failed rule={}", rule.getRuleName(), e);
            result.setMessage("Check failed: " + e.getMessage());
        }
        return result;
    }

    public List<TrackerAlertRecord> listRecords(Long ruleId, int limit) {
        LambdaQueryWrapper<TrackerAlertRecord> qw = Wrappers.lambdaQuery();
        if (ruleId != null) qw.eq(TrackerAlertRecord::getRuleId, ruleId);
        qw.orderByDesc(TrackerAlertRecord::getTriggeredAt);
        qw.last("LIMIT " + Math.min(limit, 200));
        return recordMapper.selectList(qw);
    }

    private double queryCurrent(TrackerAlertRule r) {
        return queryValue(r, "now() - INTERVAL " + esc(r.getComparisonPeriod()), "now()");
    }

    private double queryPrevious(TrackerAlertRule r) {
        String p = r.getComparisonPeriod();
        return queryValue(r, "now() - INTERVAL 2 * " + esc(p), "now() - INTERVAL " + esc(p));
    }

    private double queryValue(TrackerAlertRule r, String from, String to) {
        String aggFn = switch (r.getAggregation() != null ? r.getAggregation() : "count") {
            case "countDistinct" -> "count(DISTINCT " + r.getMetric() + ")";
            case "avg" -> "avg(" + r.getMetric() + ")";
            case "sum" -> "sum(" + r.getMetric() + ")";
            default -> "count()";
        };
        StringBuilder sql = new StringBuilder("SELECT ").append(aggFn).append(" AS v FROM gateflow_tracker.events WHERE 1=1");
        if (r.getEventType() != null) sql.append(" AND event_type = '").append(esc(r.getEventType())).append("'");
        sql.append(" AND timestamp BETWEEN ").append(from).append(" AND ").append(to);
        if (r.getDimensionFilters() != null && !r.getDimensionFilters().isEmpty()) {
            try {
                Map<String, String> f = objectMapper.readValue(r.getDimensionFilters(), new TypeReference<>() {});
                for (var e : f.entrySet()) sql.append(" AND ").append(esc(e.getKey())).append(" = '").append(esc(e.getValue())).append("'");
            } catch (Exception ignored) {}
        }
        Map<String, Object> row = chJdbc.queryForMap(sql.toString(), new MapSqlParameterSource());
        Object v = row.get("v");
        return v != null ? ((Number) v).doubleValue() : 0;
    }

    private String esc(String s) { return s != null ? s.replace("'", "''") : ""; }
}
