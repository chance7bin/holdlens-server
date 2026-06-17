package com.echoamoy.holdlens.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioFundDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private List<HoldingDetail> holdings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldingDetail implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long holdingId;
        private Long accountId;
        private String accountName;
        private String accountType;
        private Long assetId;
        private String assetCode;
        private String assetName;
        private String assetKind;
        private String assetType;
        private String assetCategory;
        private String holdingSource;
        private BigDecimal amount;
        private String currency;
        private String amountDisplay;
        private String amountMissingReason;
        private String status;
        private FundDetail fundDetail;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundDetail implements Serializable {

        private static final long serialVersionUID = 1L;

        private String fundCode;
        private String fundName;
        private String detailStatus;
        private String buyStatus;
        private String dailyPurchaseLimit;
        private Date generatedAt;
        private Date returnsAsOf;
        private Date topHoldingsAsOf;
        private String publicHoldingsStatus;
        private BigDecimal oneMonthReturn;
        private BigDecimal threeMonthsReturn;
        private BigDecimal sixMonthsReturn;
        private BigDecimal oneYearReturn;
        private BigDecimal threeYearsReturn;
        private String fieldSourcesJson;
        private String missingReasonsJson;
        private List<TopHolding> topHoldings;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopHolding implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer rankNo;
        private String stockName;
        private String stockCode;
        private String market;
        private BigDecimal dailyReturn;
        private BigDecimal holdingRatio;
        private String quarterChangeType;
        private BigDecimal quarterChangeValue;
        private String missingReasonsJson;

    }

}
