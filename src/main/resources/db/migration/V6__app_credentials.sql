-- V6__app_credentials.sql
-- 为 SPM 应用层添加 appKey + appSecret，用于采集端点 HMAC 签名验证

ALTER TABLE tracker_spm
    ADD COLUMN app_key VARCHAR(64) DEFAULT NULL COMMENT '应用公钥',
    ADD COLUMN app_secret VARCHAR(128) DEFAULT NULL COMMENT '应用密钥（HMAC签名用）';

UPDATE tracker_spm SET app_key = CONCAT('ak_', LOWER(REPLACE(spm_code, '.', '_'))) WHERE app_key IS NULL;
