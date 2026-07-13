package com.echoamoy.holdlens.server.cases.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundSliceRefreshCallbackCommand {
    private String schemaVersion;
    private String serverTaskId;
    private String idempotencyKey;
    private String status;
    private String generatedAt;
    private List<FundItem> funds;
    private List<RefreshWarning> refreshWarnings;
    private String errorSummary;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundItem {
        private String fundCode;
        private String fundName;
        private String fundType;
        private String pinyinAbbr;
        private String pinyinFull;
        private String buyStatus;
        private String dailyPurchaseLimit;
        private String coverageStatus;
        private String returnsAsOf;
        private BigDecimal unitNav;
        private BigDecimal accumulatedNav;
        private BigDecimal dailyGrowthRate;
        private BigDecimal oneMonthReturn;
        private BigDecimal threeMonthsReturn;
        private BigDecimal sixMonthsReturn;
        private BigDecimal oneYearReturn;
        private BigDecimal threeYearsReturn;
        private String topHoldingsAsOf;
        private String publicHoldingsStatus;
        private List<TopHolding> topHoldings;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopHolding {
        private Integer rankNo;
        private String stockName;
        private String stockCode;
        private String market;
        private BigDecimal holdingRatio;
        private String quarterChangeType;
        private BigDecimal quarterChangeValue;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RefreshWarning {
        private String severity;
        private String module;
        private String event;
        private String message;
    }
}
