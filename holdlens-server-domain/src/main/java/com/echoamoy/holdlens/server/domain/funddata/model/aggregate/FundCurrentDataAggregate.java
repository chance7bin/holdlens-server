package com.echoamoy.holdlens.server.domain.funddata.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundCurrentDataAggregate {

    private String schemaVersion;
    private Date generatedAt;
    private String status;
    private String sourceRefId;
    private List<FundDetail> funds;
    private List<RefreshWarning> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundDetail {
        private Long id;
        private String fundCode;
        private String fundName;
        private String buyStatus;
        private String dailyPurchaseLimit;
        private Date returnsAsOf;
        private Date topHoldingsAsOf;
        private String publicHoldingsStatus;
        private BigDecimal oneMonthReturn;
        private BigDecimal threeMonthsReturn;
        private BigDecimal sixMonthsReturn;
        private BigDecimal oneYearReturn;
        private BigDecimal threeYearsReturn;
        private Date generatedAt;
        private List<TopHolding> topHoldings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopHolding {
        private String fundCode;
        private Integer rankNo;
        private String stockName;
        private String stockCode;
        private String market;
        private BigDecimal holdingRatio;
        private String quarterChangeType;
        private BigDecimal quarterChangeValue;
        private StockQuote stockQuote;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockQuote {
        private String stockCode;
        private String market;
        private String stockName;
        private Date tradeDate;
        private BigDecimal dailyReturn;
        private Date quoteTime;
        private Date updateTime;
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
