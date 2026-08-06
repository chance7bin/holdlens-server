package com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj;

public enum BookkeepingGranularityEnumVO {
    WEEK, MONTH, YEAR;

    public static BookkeepingGranularityEnumVO from(String value) {
        try {
            return value == null ? null : valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("统计粒度不合法");
        }
    }
}
