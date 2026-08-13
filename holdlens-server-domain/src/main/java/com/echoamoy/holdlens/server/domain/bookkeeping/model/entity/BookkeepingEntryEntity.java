package com.echoamoy.holdlens.server.domain.bookkeeping.model.entity;

import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryStatusEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookkeepingEntryEntity {

    public static final String CURRENCY_CNY = "CNY";

    private Long id;
    private Long userId;
    private String requestId;
    private BookkeepingEntryTypeEnumVO type;
    private String categoryCode;
    private String categoryName;
    private String categoryIconKey;
    private BigDecimal amount;
    private String currency;
    private LocalDate entryDate;
    private String note;
    private BookkeepingEntryStatusEnumVO status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static BookkeepingEntryEntity create(Long userId, String requestId, BookkeepingEntryTypeEnumVO type,
                                                String categoryCode, BigDecimal amount, LocalDate entryDate, String note) {
        validateUserId(userId);
        validateRequestId(requestId);
        validateEditable(type, categoryCode, amount, entryDate, note);
        return builder()
                .userId(userId)
                .requestId(requestId.trim())
                .type(type)
                .categoryCode(categoryCode)
                .amount(amount)
                .currency(CURRENCY_CNY)
                .entryDate(entryDate)
                .note(normalizeNote(note))
                .status(BookkeepingEntryStatusEnumVO.ACTIVE)
                .build();
    }

    public void revise(BookkeepingEntryTypeEnumVO type, String categoryCode, BigDecimal amount, LocalDate entryDate, String note) {
        ensureActive();
        validateEditable(type, categoryCode, amount, entryDate, note);
        this.type = type;
        this.categoryCode = categoryCode;
        this.amount = amount;
        this.entryDate = entryDate;
        this.note = normalizeNote(note);
    }

    public void delete() {
        ensureActive();
        this.status = BookkeepingEntryStatusEnumVO.DELETED;
    }

    public void ensureActive() {
        if (status != BookkeepingEntryStatusEnumVO.ACTIVE) {
            throw new IllegalArgumentException("收支条目不存在或不可见");
        }
    }

    public static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户ID不合法");
        }
    }

    public static void validateEditable(BookkeepingEntryTypeEnumVO type, String categoryCode, BigDecimal amount, LocalDate entryDate, String note) {
        if (type == null) {
            throw new IllegalArgumentException("收支类型不合法");
        }
        if (categoryCode == null || categoryCode.isBlank() || categoryCode.length() > 50) {
            throw new IllegalArgumentException("收支分类不合法");
        }
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || Math.max(amount.precision() - amount.scale(), 0) > 18) {
            throw new IllegalArgumentException("金额不合法");
        }
        if (entryDate == null || entryDate.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))) {
            throw new IllegalArgumentException("发生日期不合法");
        }
        normalizeNote(note);
    }

    public static void validateRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty() || requestId.trim().length() > 64) {
            throw new IllegalArgumentException("请求幂等键不合法");
        }
    }

    private static String normalizeNote(String note) {
        String value = note == null ? null : note.trim();
        if (value != null && value.length() > 200) {
            throw new IllegalArgumentException("备注过长");
        }
        return value == null || value.isEmpty() ? null : value;
    }
}
