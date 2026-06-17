package com.echoamoy.holdlens.server.api.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentFundRefreshCallbackRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("schema_version")
    private String schemaVersion;

    @JsonProperty("server_task_id")
    private String serverTaskId;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    private String status;

    @JsonProperty("generated_at")
    private String generatedAt;

    private List<FundDetail> funds;

    @JsonProperty("data_sources")
    private List<DataSourceMeta> dataSources;

    @JsonProperty("refresh_warnings")
    private List<RefreshWarning> refreshWarnings;

    @JsonProperty("error_summary")
    private String errorSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundDetail implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("fund_code")
        private String fundCode;

        @JsonProperty("fund_name")
        private String fundName;

        @JsonProperty("buy_status")
        private String buyStatus;

        @JsonProperty("daily_purchase_limit")
        private String dailyPurchaseLimit;

        @JsonProperty("returns_as_of")
        private String returnsAsOf;

        @JsonProperty("top_holdings_as_of")
        private String topHoldingsAsOf;

        @JsonProperty("public_holdings_status")
        private String publicHoldingsStatus;

        @JsonProperty("one_month_return")
        private BigDecimal oneMonthReturn;

        @JsonProperty("three_months_return")
        private BigDecimal threeMonthsReturn;

        @JsonProperty("six_months_return")
        private BigDecimal sixMonthsReturn;

        @JsonProperty("one_year_return")
        private BigDecimal oneYearReturn;

        @JsonProperty("three_years_return")
        private BigDecimal threeYearsReturn;

        @JsonProperty("field_sources")
        private Map<String, Object> fieldSources;

        @JsonProperty("missing_reasons")
        private Map<String, Object> missingReasons;

        @JsonProperty("top_holdings")
        private List<TopHolding> topHoldings;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopHolding implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("rank_no")
        private Integer rankNo;

        @JsonProperty("stock_name")
        private String stockName;

        @JsonProperty("stock_code")
        private String stockCode;

        private String market;

        @JsonProperty("daily_return")
        private BigDecimal dailyReturn;

        @JsonProperty("holding_ratio")
        private BigDecimal holdingRatio;

        @JsonProperty("quarter_change_type")
        private String quarterChangeType;

        @JsonProperty("quarter_change_value")
        private BigDecimal quarterChangeValue;

        @JsonProperty("missing_reasons")
        private Map<String, Object> missingReasons;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceMeta implements Serializable {

        private static final long serialVersionUID = 1L;

        private String provider;

        private String name;

        private String url;

        @JsonProperty("fetched_at")
        private String fetchedAt;

        private String status;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshWarning implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("fund_code")
        private String fundCode;

        private String code;

        private String message;

        private String severity;

        @JsonProperty("source_section")
        private String sourceSection;

        @JsonAlias("row_number")
        @JsonProperty("source_row_number")
        private Integer sourceRowNumber;

    }

}
