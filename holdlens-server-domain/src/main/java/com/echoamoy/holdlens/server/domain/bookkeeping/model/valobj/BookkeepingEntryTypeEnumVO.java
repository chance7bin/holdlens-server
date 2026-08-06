package com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj;

public enum BookkeepingEntryTypeEnumVO {
    EXPENSE, INCOME;

    public static BookkeepingEntryTypeEnumVO from(String value) {
        try {
            return value == null ? null : valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("收支类型不合法");
        }
    }
}
