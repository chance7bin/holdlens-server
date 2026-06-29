package com.echoamoy.holdlens.server.types.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static com.echoamoy.holdlens.server.types.common.StringUtils.isBlank;

public class DateTimeUtils {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private DateTimeUtils() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(BUSINESS_ZONE);
    }

    public static LocalDateTime parseOffsetDateTimeOrNow(String value) {
        if (isBlank(value)) {
            return now();
        }
        try {
            return toBusinessLocalDateTime(value);
        } catch (DateTimeParseException e) {
            return now();
        }
    }

    public static LocalDateTime parseOffsetDateTimeOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return toBusinessLocalDateTime(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 将跨系统输入中的 offset-aware 时间归一化为业务本地时间，用于写入 MySQL DATETIME。
     */
    public static LocalDateTime toBusinessLocalDateTime(String value) {
        return OffsetDateTime.parse(value)
                .atZoneSameInstant(BUSINESS_ZONE)
                .toLocalDateTime();
    }

    public static Date toBusinessDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atZone(BUSINESS_ZONE).toInstant());
    }

}
