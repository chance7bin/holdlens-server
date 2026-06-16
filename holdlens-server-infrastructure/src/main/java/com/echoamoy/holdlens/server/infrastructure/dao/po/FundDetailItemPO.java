package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundDetailItemPO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long snapshotId;
    private Long userId;
    private Long fundAssetId;
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
    private Date createTime;
    private Date updateTime;

}
