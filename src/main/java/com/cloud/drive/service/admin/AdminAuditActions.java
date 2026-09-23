package com.cloud.drive.service.admin;

public final class AdminAuditActions {
    private AdminAuditActions() {}

    public static final String ADMIN_LOGIN = "ADMIN_LOGIN";
    public static final String USER_DISABLE = "USER_DISABLE";
    public static final String USER_DELETE = "USER_DELETE";
    public static final String FILE_DELETE = "FILE_DELETE";
    public static final String FILE_RESTORE = "FILE_RESTORE";
    public static final String FILE_PURGE = "FILE_PURGE";
    public static final String SHARE_REVOKE = "SHARE_REVOKE";
    public static final String PLAN_OVERRIDE = "PLAN_OVERRIDE";
    public static final String SUBSCRIPTION_EXTEND = "SUBSCRIPTION_EXTEND";
    public static final String SUBSCRIPTION_CANCEL = "SUBSCRIPTION_CANCEL";
    public static final String USAGE_RESET = "USAGE_RESET";
    public static final String WEBHOOK_REPLAY = "WEBHOOK_REPLAY";
}
