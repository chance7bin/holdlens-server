package com.echoamoy.holdlens.server.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AssetRequestDTO {

    private AssetRequestDTO() { }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateCatalog {
        @NotNull @Positive private Long userId;
        private Long parentId;
        @NotBlank private String catalogName;
        @NotBlank private String balanceDirection;
        private Integer sortOrder;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateCatalog {
        @NotNull @Positive private Long userId;
        private Long parentId;
        @NotBlank private String catalogName;
        @NotBlank private String balanceDirection;
        private Integer sortOrder;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateRecord {
        @NotNull @Positive private Long userId;
        @NotNull @Positive private Long catalogId;
        private String assetRef;
        private String recordName;
        @NotNull @PositiveOrZero private BigDecimal amount;
        @NotBlank private String currency;
        private String remark;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateDetails {
        @NotNull @Positive private Long userId;
        private String recordName;
        private String remark;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateAmount {
        @NotNull @Positive private Long userId;
        @NotNull @PositiveOrZero private BigDecimal amount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserOperation {
        @NotNull @Positive private Long userId;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SplitRecord {
        @NotNull @Positive private Long userId;
        @NotBlank private String assetRef;
        @NotNull @Positive private BigDecimal amount;
        private String remark;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpsertExchangeRate {
        @NotBlank private String baseCurrency;
        @NotBlank private String quoteCurrency;
        @NotNull @Positive private BigDecimal rate;
        private String source;
        private LocalDateTime sourceAsOf;
        private LocalDateTime fetchedAt;
    }
}
