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
public class FundDetailSnapshotAggregate {

    private Long snapshotId;
    private String schemaVersion;
    private Date generatedAt;
    private String snapshotStatus;
    private String sourceType;
    private String sourceRefId;
    private String dataSourcesJson;
    private List<FundDetail> funds;
    private List<RefreshWarning> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FundDetail {
        private Long id;
        private Long snapshotId;
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
        private String fieldSourcesJson;
        private String missingReasonsJson;
        private Date generatedAt;
        private List<TopHolding> topHoldings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopHolding {
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshWarning {
        private String fundCode;
        private String code;
        private String message;
        private String severity;
        private String sourceSection;
        private Integer sourceRowNumber;
    }

}
