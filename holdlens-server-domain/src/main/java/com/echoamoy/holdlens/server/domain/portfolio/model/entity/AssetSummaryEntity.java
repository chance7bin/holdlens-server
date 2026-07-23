package com.echoamoy.holdlens.server.domain.portfolio.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSummaryEntity {

    private String targetCurrency;
    private BigDecimal assetTotal;
    private BigDecimal liabilityTotal;
    private BigDecimal netAsset;
    private boolean partial;
    private List<String> missingCurrencies;
    private List<UnconvertedAmount> unconvertedAmounts;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnconvertedAmount {
        private String currency;
        private BigDecimal amount;
    }
}
