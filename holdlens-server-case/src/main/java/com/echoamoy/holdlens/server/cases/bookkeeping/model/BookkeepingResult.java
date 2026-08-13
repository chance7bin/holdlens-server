package com.echoamoy.holdlens.server.cases.bookkeeping.model;

import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingCategoryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class BookkeepingResult {

    private BookkeepingResult() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryList {
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
        private List<BookkeepingEntryEntity> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        private BookkeepingEntryTypeEnumVO type;
        private String granularity;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private BigDecimal total;
        private BigDecimal average;
        private List<Point> points;
        private List<Category> categories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String label;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category {
        private String categoryCode;
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal ratio;
        private String categoryIconKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySettings {
        private List<BookkeepingCategoryEntity> enabled;
        private List<BookkeepingCategoryEntity> disabled;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyBill {
        private Integer year;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
        private List<Month> months;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Month {
        private Integer month;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearlyBill {
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
        private List<Year> years;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Year {
        private Integer year;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
    }
}
