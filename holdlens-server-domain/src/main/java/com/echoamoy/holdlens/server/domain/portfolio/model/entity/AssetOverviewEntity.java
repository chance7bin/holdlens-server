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
public class AssetOverviewEntity {

    public static final String CONVERSION_CONVERTED = "CONVERTED";
    public static final String CONVERSION_MISSING_RATE = "MISSING_RATE";

    private AssetSummaryEntity summary;
    private List<CatalogAmount> catalogs;
    private List<RecordAmount> records;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CatalogAmount {
        private AssetCatalogEntity catalog;
        private BigDecimal convertedAmount;
        private boolean partial;
        private List<String> missingCurrencies;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecordAmount {
        private AssetRecordEntity record;
        private BigDecimal convertedAmount;
        private String conversionStatus;
    }
}
