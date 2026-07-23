package com.echoamoy.holdlens.server.cases.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AssetManagementCommand {

    private AssetManagementCommand() { }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateCatalog {
        private Long userId;
        private Long parentId;
        private String catalogName;
        private String balanceDirection;
        private Integer sortOrder;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateCatalog {
        private Long userId;
        private Long catalogId;
        private Long parentId;
        private String catalogName;
        private String balanceDirection;
        private Integer sortOrder;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateRecord {
        private Long userId;
        private Long catalogId;
        private String assetRef;
        private String recordName;
        private BigDecimal amount;
        private String currency;
        private String remark;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateDetails {
        private Long userId;
        private Long recordId;
        private String recordName;
        private String remark;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateAmount {
        private Long userId;
        private Long recordId;
        private BigDecimal amount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SplitRecord {
        private Long userId;
        private Long sourceRecordId;
        private String assetRef;
        private BigDecimal amount;
        private String remark;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpsertExchangeRate {
        private String baseCurrency;
        private String quoteCurrency;
        private BigDecimal rate;
        private String source;
        private LocalDateTime sourceAsOf;
        private LocalDateTime fetchedAt;
    }
}
