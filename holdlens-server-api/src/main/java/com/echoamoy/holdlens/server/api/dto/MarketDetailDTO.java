package com.echoamoy.holdlens.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public final class MarketDetailDTO {
    private MarketDetailDTO() { }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Task { private String serverTaskId; private String taskType; private String status; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundNavHistory { private String fundCode; private String period; private String asOf; private List<FundNavPoint> points; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundNavPoint { private String navDate; private BigDecimal unitNav; private BigDecimal accumulatedNav; private BigDecimal dailyGrowthRate; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundPeriodPerformance {
        private String fundCode; private String asOf; private List<FundPeriodPerformanceRow> rows;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundPeriodPerformanceRow {
        private String period; private BigDecimal fundReturn; private BigDecimal peerAverage;
        private Integer peerRank; private Integer peerTotal; private Integer rankChange;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundDetailRefresh {
        private String fundCode; private String status; private Long retryAfterMs; private List<FundDetailSlice> slices;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundDetailSlice { private String slice; private String status; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockDetailRefresh {
        private String assetRef; private String serverTaskId; private String status; private Long retryAfterMs;
        private List<StockDetailSlice> slices;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockDetailSlice { private String slice; private String status; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockPriceHistory {
        private String assetRef; private String period; private String granularity; private String currency;
        private String asOf; private List<StockBar> points;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockBar {
        private String barTime; private BigDecimal open; private BigDecimal high; private BigDecimal low;
        private BigDecimal close; private BigDecimal volume;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockCompanyProfile {
        private String assetRef; private String companyName; private String industry; private String businessSummary;
        private String companyProfile; private String website; private String asOf;
    }
}
