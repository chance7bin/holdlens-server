package com.echoamoy.holdlens.server.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public final class MarketDetailRefreshRequest {
    private MarketDetailRefreshRequest() { }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Create {
        private String assetKind; private String assetRef; private List<String> slices; private List<String> periods;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Callback {
        @JsonProperty("schema_version") private String schemaVersion;
        @JsonProperty("server_task_id") private String serverTaskId;
        @JsonProperty("idempotency_key") private String idempotencyKey;
        private String status;
        @JsonProperty("generated_at") private String generatedAt;
        @JsonProperty("asset_kind") private String assetKind;
        @JsonProperty("asset_ref") private String assetRef;
        @JsonProperty("fund_nav_history") private FundNavHistory fundNavHistory;
        @JsonProperty("stock_price_histories") private List<StockPriceHistory> stockPriceHistories;
        @JsonProperty("stock_company_profile") private StockCompanyProfile stockCompanyProfile;
        @JsonProperty("refresh_warnings") private List<RefreshWarning> refreshWarnings;
        @JsonProperty("error_summary") private String errorSummary;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundNavHistory { @JsonProperty("fund_code") private String fundCode; private List<FundNavPoint> points; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class FundNavPoint {
        @JsonProperty("nav_date") private String navDate;
        @JsonProperty("unit_nav") private String unitNav;
        @JsonProperty("accumulated_nav") private String accumulatedNav;
        @JsonProperty("daily_growth_rate") private String dailyGrowthRate;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockPriceHistory {
        private String period; private String granularity; private String currency; private List<StockBar> bars;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockBar {
        @JsonProperty("bar_time") private String barTime;
        private String open; private String high; private String low; private String close; private String volume;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockCompanyProfile {
        @JsonProperty("company_name") private String companyName;
        private String industry;
        @JsonProperty("business_summary") private String businessSummary;
        @JsonProperty("company_profile") private String companyProfile;
        private String website;
        @JsonProperty("source_as_of") private String sourceAsOf;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RefreshWarning { private String module; private String event; private String message; private String severity; }
}
