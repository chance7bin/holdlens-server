package com.echoamoy.holdlens.server.domain.bookkeeping.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public final class BookkeepingBillEntity {

    private BookkeepingBillEntity() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Monthly {
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
    public static class Yearly {
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
