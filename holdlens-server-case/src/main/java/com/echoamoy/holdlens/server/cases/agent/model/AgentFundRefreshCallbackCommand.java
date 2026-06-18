package com.echoamoy.holdlens.server.cases.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentFundRefreshCallbackCommand {

    private String schemaVersion;

    private String serverTaskId;

    private String idempotencyKey;

    private String status;

    private String generatedAt;

    private List<FundDetail> funds;

    private List<DataSourceMeta> dataSources;

    private List<RefreshWarning> refreshWarnings;

    private String errorSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundDetail {

        private String fundCode;

        private String fundName;

        private String buyStatus;

        private String dailyPurchaseLimit;

        private String returnsAsOf;

        private String topHoldingsAsOf;

        private String publicHoldingsStatus;

        private BigDecimal oneMonthReturn;

        private BigDecimal threeMonthsReturn;

        private BigDecimal sixMonthsReturn;

        private BigDecimal oneYearReturn;

        private BigDecimal threeYearsReturn;

        private Map<String, Object> fieldSources;

        private Map<String, Object> missingReasons;

        private List<TopHolding> topHoldings;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopHolding {

        private Integer rankNo;

        private String stockName;

        private String stockCode;

        private String market;

        private BigDecimal dailyReturn;

        private BigDecimal holdingRatio;

        private String quarterChangeType;

        private BigDecimal quarterChangeValue;

        private Map<String, Object> missingReasons;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceMeta {

        private String provider;

        private String name;

        private String url;

        private String fetchedAt;

        private String status;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshWarning {

        private String module;

        private String event;

        private String message;

        private String severity;

    }

}
