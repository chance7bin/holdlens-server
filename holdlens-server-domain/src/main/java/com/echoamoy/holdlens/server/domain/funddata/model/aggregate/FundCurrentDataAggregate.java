package com.echoamoy.holdlens.server.domain.funddata.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundCurrentDataAggregate {

    private String schemaVersion;
    private LocalDateTime generatedAt;
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
        private String fundType;
        private String pinyinAbbr;
        private String pinyinFull;
        private String buyStatus;
        private String dailyPurchaseLimit;
        private String returnCoverageStatus;
        private Date returnsAsOf;
        private BigDecimal unitNav;
        private BigDecimal accumulatedNav;
        private BigDecimal dailyGrowthRate;
        private Date topHoldingsAsOf;
        private String publicHoldingsStatus;
        private Date assetAllocationAsOf;
        private String assetAllocationStatus;
        private BigDecimal oneMonthReturn;
        private BigDecimal threeMonthsReturn;
        private BigDecimal sixMonthsReturn;
        private BigDecimal oneYearReturn;
        private BigDecimal threeYearsReturn;
        private LocalDateTime catalogFetchedAt;
        private LocalDateTime purchaseStatusFetchedAt;
        private LocalDateTime periodReturnFetchedAt;
        private LocalDateTime topHoldingFetchedAt;
        private LocalDateTime assetAllocationFetchedAt;
        private LocalDateTime lastDetailViewTime;
        private LocalDateTime updateTime;
        private List<TopHolding> topHoldings;
        private List<AssetAllocation> assetAllocations;
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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetAllocation {
        private String fundCode;
        private String assetType;
        private String assetTypeName;
        private BigDecimal allocationRatio;
        private Integer displayOrder;
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
