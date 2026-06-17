-- 管理操作审计日志:记录所有变更类操作(谁/何时/对什么/结果)
CREATE TABLE IF NOT EXISTS tracker_audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL COMMENT '操作者用户名',
    role        VARCHAR(32)  DEFAULT NULL COMMENT '操作者角色',
    method      VARCHAR(8)   NOT NULL COMMENT 'HTTP 方法',
    path        VARCHAR(512) NOT NULL COMMENT '请求路径',
    status      INT          NOT NULL COMMENT 'HTTP 响应状态码',
    request_id  VARCHAR(64)  DEFAULT NULL COMMENT '关联 X-Request-Id',
    ip          VARCHAR(64)  DEFAULT NULL COMMENT '客户端 IP',
    duration_ms BIGINT       DEFAULT NULL COMMENT '处理耗时(ms)',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
    PRIMARY KEY (id),
    KEY idx_audit_username (username),
    KEY idx_audit_created_at (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='管理操作审计日志';
