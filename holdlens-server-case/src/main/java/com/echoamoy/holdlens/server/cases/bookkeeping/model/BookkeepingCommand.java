package com.echoamoy.holdlens.server.cases.bookkeeping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class BookkeepingCommand {

    private BookkeepingCommand() {
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private Long userId;
        private String requestId;
        private String type;
        private String categoryCode;
        private BigDecimal amount;
        private LocalDate entryDate;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Revise {
        private Long userId;
        private Long entryId;
        private String type;
        private String categoryCode;
        private BigDecimal amount;
        private LocalDate entryDate;
        private String note;
    }
}
