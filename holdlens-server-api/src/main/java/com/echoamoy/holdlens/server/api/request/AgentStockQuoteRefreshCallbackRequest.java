package com.echoamoy.holdlens.server.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStockQuoteRefreshCallbackRequest implements Serializable {

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

    private List<StockQuote> quotes;

    @JsonProperty("refresh_warnings")
    private List<RefreshWarning> refreshWarnings;

    @JsonProperty("error_summary")
    private String errorSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockQuote implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("stock_code")
        private String stockCode;

        private String market;

        @JsonProperty("stock_name")
        private String stockName;

        @JsonProperty("trade_date")
        private String tradeDate;

        @JsonProperty("daily_return")
        private BigDecimal dailyReturn;

        @JsonProperty("quote_time")
        private String quoteTime;

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
