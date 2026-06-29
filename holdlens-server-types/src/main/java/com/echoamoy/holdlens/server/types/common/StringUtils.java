package com.echoamoy.holdlens.server.types.common;

public class StringUtils {

    private StringUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String normalizeNullable(String value) {
        return isBlank(value) ? null : value.trim();
    }

    public static String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

}
