-- 告警规则与触发历史(数据质量监控 + 告警引擎)
CREATE TABLE IF NOT EXISTS tracker_alert_rule (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(128) NOT NULL COMMENT '规则名',
    app_code       VARCHAR(64)  DEFAULT NULL COMMENT '目标 app(空=全部)',
    metric         VARCHAR(32)  NOT NULL COMMENT 'event_volume_drop|error_rate|null_rate',
    threshold      DOUBLE       NOT NULL COMMENT '阈值(跌幅%或比率)',
    window_minutes INT          NOT NULL DEFAULT 60 COMMENT '观测窗口(分钟)',
    enabled        TINYINT      NOT NULL DEFAULT 1,
    notify_channel VARCHAR(32)  DEFAULT NULL COMMENT 'webhook',
    notify_target  VARCHAR(512) DEFAULT NULL COMMENT '通知地址',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='告警规则';

CREATE TABLE IF NOT EXISTS tracker_alert_event (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    rule_id    BIGINT       NOT NULL,
    rule_name  VARCHAR(128) DEFAULT NULL,
    metric     VARCHAR(32)  DEFAULT NULL,
    app_code   VARCHAR(64)  DEFAULT NULL,
    observed   DOUBLE       DEFAULT NULL COMMENT '观测值',
    threshold  DOUBLE       DEFAULT NULL,
    message    VARCHAR(512) DEFAULT NULL,
    fired_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_alert_fired (fired_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='告警触发历史';
