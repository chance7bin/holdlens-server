package com.echoamoy.holdlens.server.cases.marketdetail.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public final class MarketDetailCommand {
    private MarketDetailCommand() { }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateTask {
        private String assetKind;
        private String assetRef;
        private List<String> slices;
        private List<String> periods;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Callback {
        private String schemaVersion;
        private String serverTaskId;
        private String idempotencyKey;
        private String status;
        private String generatedAt;
        private String assetKind;
        private String assetRef;
        private FundNavHistory fundNavHistory;
        private List<StockPriceHistory> stockPriceHistories;
        private StockCompanyProfile stockCompanyProfile;
        private List<RefreshWarning> refreshWarnings;
        private String errorSummary;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundNavHistory { private String fundCode; private List<FundNavPoint> points; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundNavPoint {
        private String navDate; private String unitNav; private String accumulatedNav; private String dailyGrowthRate;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockPriceHistory {
        private String period; private String granularity; private String currency; private List<StockBar> bars;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockBar {
        private String barTime; private String open; private String high; private String low; private String close; private String volume;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockCompanyProfile {
        private String companyName; private String industry; private String businessSummary;
        private String companyProfile; private String website; private String sourceAsOf;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RefreshWarning {
        private String module; private String event; private String message; private String severity;
    }
}
