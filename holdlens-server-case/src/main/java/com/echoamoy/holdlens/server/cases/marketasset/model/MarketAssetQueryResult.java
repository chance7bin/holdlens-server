package com.echoamoy.holdlens.server.cases.marketasset.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public final class MarketAssetQueryResult {

    private MarketAssetQueryResult() { }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Watchlist {
        private Integer fundCount;
        private Integer stockCount;
        private List<Item> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Search {
        private List<Item> items;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        private String assetKind;
        private String assetRef;
        private String code;
        private String name;
        private String assetType;
        private String market;
        private String marketLabel;
        private String currency;
        private BigDecimal latestValue;
        private BigDecimal changePercent;
        private String valueAsOf;
        private Boolean watchlisted;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StockDetail {
        private String assetKind;
        private String assetRef;
        private String code;
        private String name;
        private String market;
        private String marketLabel;
        private String currency;
        private BigDecimal latestPrice;
        private BigDecimal changeAmount;
        private BigDecimal changePercent;
        private BigDecimal openPrice;
        private BigDecimal highPrice;
        private BigDecimal lowPrice;
        private BigDecimal previousClose;
        private Long volume;
        private String volumeUnit;
        private BigDecimal peRatio;
        private BigDecimal totalMarketValue;
        private String quoteAsOf;
        private String delayNotice;
        private Boolean watchlisted;
    }
}
