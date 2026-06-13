package com.zerocode.platform.exception;

public enum ErrorCode {

    // Auth
    AUTH_UNAUTHORIZED("AUTH_001", "Not authenticated"),
    AUTH_FORBIDDEN("AUTH_002", "Access denied"),
    AUTH_CREDENTIALS_INVALID("AUTH_003", "Invalid username or password"),
    AUTH_USERNAME_EXISTS("AUTH_004", "Username already exists"),

    // Resource
    RESOURCE_NOT_FOUND("RES_001", "Resource not found"),
    APP_NOT_FOUND("RES_002", "App not found"),
    VERSION_NOT_FOUND("RES_003", "Version not found"),

    // Validation
    VALIDATION_ERROR("VAL_001", "Invalid request"),
    INVALID_PROJECT("VAL_002", "Invalid project"),
    UNSAFE_FILE_PATH("VAL_003", "Invalid file path"),
    DUPLICATE_FILE_PATH("VAL_004", "Duplicate file path"),
    EXTERNAL_URL_DETECTED("VAL_005", "Project file must not reference external URLs"),
    NETWORK_REQUEST_DETECTED("VAL_006", "Project file must not perform network requests"),
    DANGEROUS_CODE_DETECTED("VAL_007", "Project file must not use dynamic code execution"),

    // Upstream
    UPSTREAM_UNAVAILABLE("UP_001", "Upstream service unavailable"),

    // Rate limit
    RATE_LIMITED("RATE_001", "Rate limit exceeded"),

    // Server
    INTERNAL_ERROR("SYS_001", "Internal server error");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String code() { return code; }
    public String defaultMessage() { return defaultMessage; }
}
