package com.echoamoy.holdlens.server.cases.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AShareMarketRefreshCallbackCommand {

    private String schemaVersion;
    private String serverTaskId;
    private String idempotencyKey;
    private String status;
    private String generatedAt;
    private String market;
    private List<StockMarket> stocks;
    private List<RefreshWarning> refreshWarnings;
    private String errorSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockMarket {
        private String stockCode;
        private String stockName;
        private String market;
        private String status;
        private String exchangeCode;
        private String providerMarketCode;
        private String currency;
        private String volumeUnit;
        private String latestPrice;
        private String changePercent;
        private String changeAmount;
        private String volume;
        private String turnoverAmount;
        private String amplitude;
        private String highPrice;
        private String lowPrice;
        private String openPrice;
        private String previousClose;
        private String volumeRatio;
        private String turnoverRate;
        private String peDynamic;
        private String pbRatio;
        private String totalMarketValue;
        private String circulatingMarketValue;
        private String speed;
        private String fiveMinuteChange;
        private String sixtyDayChangePercent;
        private String yearToDateChangePercent;
        private String refreshedAt;
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
