package com.echoamoy.holdlens.server.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AShareMarketRefreshCallbackRequest implements Serializable {

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

    private String market;

    private List<StockMarket> stocks;

    @JsonProperty("refresh_warnings")
    private List<RefreshWarning> refreshWarnings;

    @JsonProperty("error_summary")
    private String errorSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockMarket implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("stock_code")
        private String stockCode;

        @JsonProperty("stock_name")
        private String stockName;

        private String market;

        private String status;

        @JsonProperty("exchange_code")
        private String exchangeCode;

        @JsonProperty("provider_market_code")
        private String providerMarketCode;

        @JsonProperty("latest_price")
        private String latestPrice;

        @JsonProperty("change_percent")
        private String changePercent;

        @JsonProperty("change_amount")
        private String changeAmount;

        private String volume;

        @JsonProperty("turnover_amount")
        private String turnoverAmount;

        private String amplitude;

        @JsonProperty("high_price")
        private String highPrice;

        @JsonProperty("low_price")
        private String lowPrice;

        @JsonProperty("open_price")
        private String openPrice;

        @JsonProperty("previous_close")
        private String previousClose;

        @JsonProperty("volume_ratio")
        private String volumeRatio;

        @JsonProperty("turnover_rate")
        private String turnoverRate;

        @JsonProperty("pe_dynamic")
        private String peDynamic;

        @JsonProperty("pb_ratio")
        private String pbRatio;

        @JsonProperty("total_market_value")
        private String totalMarketValue;

        @JsonProperty("circulating_market_value")
        private String circulatingMarketValue;

        private String speed;

        @JsonProperty("five_minute_change")
        private String fiveMinuteChange;

        @JsonProperty("sixty_day_change_percent")
        private String sixtyDayChangePercent;

        @JsonProperty("year_to_date_change_percent")
        private String yearToDateChangePercent;

        @JsonProperty("refreshed_at")
        private String refreshedAt;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshWarning implements Serializable {

        private static final long serialVersionUID = 1L;

        private String module;
        private String event;
        private String message;
        private String severity;

    }

}
