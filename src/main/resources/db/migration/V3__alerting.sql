-- V3__alerting.sql
-- 告警规则与告警记录表

CREATE TABLE IF NOT EXISTS tracker_alert_rule (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name       VARCHAR(128) NOT NULL COMMENT '规则名称',
    metric          VARCHAR(64) NOT NULL COMMENT '监控指标: count/countDistinct/avg',
    event_type      VARCHAR(64) COMMENT '事件类型过滤',
    aggregation     VARCHAR(32) DEFAULT 'count' COMMENT '聚合方式',
    dimension_filters JSON COMMENT '维度过滤',
    condition_type  VARCHAR(32) NOT NULL COMMENT '条件: above_threshold/below_threshold/decrease_pct/increase_pct',
    threshold       DECIMAL(12,2) DEFAULT 0 COMMENT '阈值',
    comparison_period VARCHAR(16) DEFAULT '1h' COMMENT '比较周期',
    check_interval  VARCHAR(16) DEFAULT '1h' COMMENT '检查间隔',
    channels        VARCHAR(256) DEFAULT 'console' COMMENT '通知渠道',
    webhook_url     VARCHAR(512) COMMENT 'Webhook地址',
    status          TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
    last_checked_at DATETIME COMMENT '上次检查时间',
    last_triggered_at DATETIME COMMENT '上次触发时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_alert_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则表';

CREATE TABLE IF NOT EXISTS tracker_alert_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id         BIGINT NOT NULL COMMENT '关联规则ID',
    rule_name       VARCHAR(128) COMMENT '规则名称',
    metric          VARCHAR(64) COMMENT '指标',
    current_value   DECIMAL(12,2) COMMENT '当前值',
    threshold_value DECIMAL(12,2) COMMENT '阈值',
    alert_message   VARCHAR(512) COMMENT '告警消息',
    triggered_at    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
    acknowledged    TINYINT DEFAULT 0 COMMENT '是否确认',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ar_rule (rule_id),
    INDEX idx_ar_time (triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警记录表';
