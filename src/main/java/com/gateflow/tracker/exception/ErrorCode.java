package com.gateflow.tracker.exception;

public class ErrorCode {

    // 参数错误 (1000-1999)
    public static final Integer PARAM_INVALID = 1001;
    public static final Integer PARAM_MISSING = 1002;

    // 业务错误 (2000-2999)
    public static final Integer EVENT_KEY_DUPLICATED = 2001;
    public static final Integer SPM_CODE_DUPLICATED = 2002;
    public static final Integer EVENT_DISABLED = 2003;
    public static final Integer PLAN_STATUS_INVALID = 2004;

    // 数据不存在 (3000-3999)
    public static final Integer EVENT_NOT_FOUND = 3001;
    public static final Integer PROPERTY_NOT_FOUND = 3002;
    public static final Integer SPM_NOT_FOUND = 3003;
    public static final Integer DASHBOARD_NOT_FOUND = 3004;
    public static final Integer PLAN_NOT_FOUND = 3005;

    // 认证授权错误 (4000-4999)
    public static final Integer AUTH_BAD_CREDENTIALS = 4001;
    public static final Integer AUTH_ACCOUNT_DISABLED = 4002;
    public static final Integer AUTH_TOKEN_EXPIRED = 4003;
    public static final Integer AUTH_TOKEN_INVALID = 4004;

    // 服务器错误 (5000-5999)
    public static final Integer INTERNAL_ERROR = 5001;
    public static final Integer CLICKHOUSE_ERROR = 5002;
}
