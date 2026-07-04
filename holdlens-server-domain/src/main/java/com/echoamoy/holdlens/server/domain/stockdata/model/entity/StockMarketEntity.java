package com.echoamoy.holdlens.server.domain.stockdata.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMarketEntity {

    public static final String MARKET_A_SHARE = "A_SHARE";
    public static final String MARKET_US_STOCK = "US_STOCK";
    public static final String CURRENCY_CNY = "CNY";
    public static final String CURRENCY_USD = "USD";
    public static final String VOLUME_UNIT_LOT = "LOT";
    public static final String VOLUME_UNIT_SHARE = "SHARE";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_MISSING_FROM_REFRESH = "missing_from_refresh";

    private Long id;
    private String stockCode;
    private String market;
    private String exchangeCode;
    private String providerMarketCode;
    private String stockName;
    private String currency;
    private String volumeUnit;
    private BigDecimal latestPrice;
    private BigDecimal changePercent;
    private BigDecimal changeAmount;
    private Long volume;
    private BigDecimal turnoverAmount;
    private BigDecimal amplitude;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal openPrice;
    private BigDecimal previousClose;
    private BigDecimal volumeRatio;
    private BigDecimal turnoverRate;
    private BigDecimal peDynamic;
    private BigDecimal peRatio;
    private BigDecimal pbRatio;
    private BigDecimal totalMarketValue;
    private BigDecimal circulatingMarketValue;
    private BigDecimal speed;
    private BigDecimal fiveMinuteChange;
    private BigDecimal sixtyDayChangePercent;
    private BigDecimal yearToDateChangePercent;
    private LocalDate listingDate;
    private String status;
    private LocalDateTime refreshedAt;

}
