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

        private Long recordId;
        private String assetRef;
        private String assetCode;
        private String assetName;
        private String assetKind;
        private String assetType;
        private BigDecimal amount;
        private String currency;
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
        private String fundType;
        private String detailStatus;
        private String buyStatus;
        private String dailyPurchaseLimit;
        private Date returnsAsOf;
        private BigDecimal unitNav;
        private BigDecimal accumulatedNav;
        private BigDecimal dailyGrowthRate;
        private String returnCoverageStatus;
        private Date topHoldingsAsOf;
        private String publicHoldingsStatus;
        private Date assetAllocationAsOf;
        private String assetAllocationStatus;
        private BigDecimal oneMonthReturn;
        private BigDecimal threeMonthsReturn;
        private BigDecimal sixMonthsReturn;
        private BigDecimal oneYearReturn;
        private BigDecimal threeYearsReturn;
        private Date catalogFetchedAt;
        private Date purchaseStatusFetchedAt;
        private Date periodReturnFetchedAt;
        private Date topHoldingFetchedAt;
        private Date assetAllocationFetchedAt;
        private String topHoldingRefreshStatus;
        private List<TopHolding> topHoldings;
        private List<AssetAllocation> assetAllocations;

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
        private BigDecimal changePercent;
        private Date refreshedAt;
        private String quoteStatus;
        private BigDecimal holdingRatio;
        private String quarterChangeType;
        private BigDecimal quarterChangeValue;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetAllocation implements Serializable {

        private static final long serialVersionUID = 1L;

        private String assetType;
        private String assetTypeName;
        private BigDecimal allocationRatio;
        private Integer displayOrder;
    }

}
