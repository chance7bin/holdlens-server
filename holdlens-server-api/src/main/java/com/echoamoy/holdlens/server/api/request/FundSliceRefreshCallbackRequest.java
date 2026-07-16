package com.echoamoy.holdlens.server.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FundSliceRefreshCallbackRequest {
    @JsonProperty("schema_version") private String schemaVersion;
    @JsonProperty("server_task_id") private String serverTaskId;
    @JsonProperty("idempotency_key") private String idempotencyKey;
    private String status;
    @JsonProperty("generated_at") private String generatedAt;
    private List<FundItem> funds;
    @JsonProperty("refresh_warnings") private List<RefreshWarning> refreshWarnings;
    @JsonProperty("error_summary") private String errorSummary;

    @Data
    public static class FundItem {
        @JsonProperty("fund_code") private String fundCode;
        @JsonProperty("fund_name") private String fundName;
        @JsonProperty("fund_type") private String fundType;
        @JsonProperty("pinyin_abbr") private String pinyinAbbr;
        @JsonProperty("pinyin_full") private String pinyinFull;
        @JsonProperty("buy_status") private String buyStatus;
        @JsonProperty("daily_purchase_limit") private String dailyPurchaseLimit;
        @JsonProperty("coverage_status") private String coverageStatus;
        @JsonProperty("returns_as_of") private String returnsAsOf;
        @JsonProperty("unit_nav") private BigDecimal unitNav;
        @JsonProperty("accumulated_nav") private BigDecimal accumulatedNav;
        @JsonProperty("daily_growth_rate") private BigDecimal dailyGrowthRate;
        @JsonProperty("one_month_return") private BigDecimal oneMonthReturn;
        @JsonProperty("three_months_return") private BigDecimal threeMonthsReturn;
        @JsonProperty("six_months_return") private BigDecimal sixMonthsReturn;
        @JsonProperty("one_year_return") private BigDecimal oneYearReturn;
        @JsonProperty("three_years_return") private BigDecimal threeYearsReturn;
        @JsonProperty("top_holdings_as_of") private String topHoldingsAsOf;
        @JsonProperty("public_holdings_status") private String publicHoldingsStatus;
        @JsonProperty("top_holdings") private List<TopHolding> topHoldings;
        @JsonProperty("asset_allocation_as_of") private String assetAllocationAsOf;
        @JsonProperty("allocation_status") private String allocationStatus;
        @JsonProperty("asset_allocations") private List<AssetAllocation> assetAllocations;
    }

    @Data
    public static class TopHolding {
        @JsonProperty("rank_no") private Integer rankNo;
        @JsonProperty("stock_name") private String stockName;
        @JsonProperty("stock_code") private String stockCode;
        private String market;
        @JsonProperty("holding_ratio") private BigDecimal holdingRatio;
        @JsonProperty("quarter_change_type") private String quarterChangeType;
        @JsonProperty("quarter_change_value") private BigDecimal quarterChangeValue;
    }

    @Data
    public static class AssetAllocation {
        @JsonProperty("asset_type") private String assetType;
        @JsonProperty("asset_type_name") private String assetTypeName;
        @JsonProperty("allocation_ratio") private BigDecimal allocationRatio;
        @JsonProperty("display_order") private Integer displayOrder;
    }

    @Data
    public static class RefreshWarning {
        private String severity;
        private String module;
        private String event;
        private String message;
    }
}
