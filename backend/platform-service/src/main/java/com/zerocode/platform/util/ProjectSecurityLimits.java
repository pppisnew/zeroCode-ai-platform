package com.zerocode.platform.util;

public final class ProjectSecurityLimits {
    public static final int MAX_PROJECT_FILES = 100;
    public static final int MAX_PROJECT_NAME_LENGTH = 128;
    public static final int MAX_FILE_PATH_LENGTH = 500;
    public static final int MAX_FILE_TYPE_LENGTH = 32;
    public static final int MAX_FILE_CONTENT_LENGTH = 200_000;

    private ProjectSecurityLimits() {
    }
}
