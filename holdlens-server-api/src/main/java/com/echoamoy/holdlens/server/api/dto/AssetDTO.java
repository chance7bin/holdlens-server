package com.echoamoy.holdlens.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AssetDTO {

    private AssetDTO() { }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Catalog {
        private Long id;
        private Long parentId;
        private String catalogCode;
        private String catalogName;
        private String catalogScope;
        private String balanceDirection;
        private Integer sortOrder;
        private String status;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Record {
        private Long id;
        private Long catalogId;
        private String catalogCode;
        private String recordName;
        private String assetKind;
        private String assetRef;
        private BigDecimal amount;
        private String currency;
        private String remark;
        private String status;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private String targetCurrency;
        private BigDecimal assetTotal;
        private BigDecimal liabilityTotal;
        private BigDecimal netAsset;
        private boolean partial;
        private List<String> missingCurrencies;
        private List<UnconvertedAmount> unconvertedAmounts;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UnconvertedAmount {
        private String currency;
        private BigDecimal amount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ExchangeRate {
        private String baseCurrency;
        private String quoteCurrency;
        private BigDecimal rate;
        private String source;
        private LocalDateTime sourceAsOf;
        private LocalDateTime fetchedAt;
    }
}
