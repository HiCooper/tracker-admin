-- 扩展 SPM 层级编码列宽 — 解决层级拼接后超长问题
-- V4__alter_code_columns_to_varchar256.sql

ALTER TABLE tracker_app      MODIFY COLUMN app_code    VARCHAR(256) NOT NULL COMMENT '应用标识';
ALTER TABLE tracker_page     MODIFY COLUMN page_code   VARCHAR(256) NOT NULL COMMENT '页面标识';
ALTER TABLE tracker_block    MODIFY COLUMN block_code  VARCHAR(256) NOT NULL COMMENT '模块标识';
ALTER TABLE tracker_function MODIFY COLUMN func_code  VARCHAR(256) NOT NULL COMMENT '功能标识';
