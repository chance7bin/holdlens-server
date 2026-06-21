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
public class AgentStockQuoteRefreshCallbackCommand {

    private String schemaVersion;
    private String serverTaskId;
    private String idempotencyKey;
    private String status;
    private String generatedAt;
    private List<StockQuote> quotes;
    private List<RefreshWarning> refreshWarnings;
    private String errorSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockQuote {
        private String stockCode;
        private String market;
        private String stockName;
        private String tradeDate;
        private BigDecimal dailyReturn;
        private String quoteTime;
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
