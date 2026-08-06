package com.echoamoy.holdlens.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class BookkeepingDTO {

    private BookkeepingDTO() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Category {
        private String code;
        private String name;
        private String type;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private Long id;
        private String type;
        private String categoryCode;
        private String categoryName;
        private BigDecimal amount;
        private String currency;
        private LocalDate entryDate;
        private String note;
        private String status;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
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
        private List<Entry> entries;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        private String type;
        private String granularity;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private BigDecimal total;
        private BigDecimal average;
        private List<StatisticPoint> points;
        private List<CategoryAmount> categories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatisticPoint {
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String label;
        private BigDecimal amount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryAmount {
        private String categoryCode;
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal ratio;
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
        private List<MonthBill> months;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthBill {
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
        private List<YearBill> years;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YearBill {
        private Integer year;
        private BigDecimal income;
        private BigDecimal expense;
        private BigDecimal balance;
    }
}
