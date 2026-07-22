package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FundNavHistoryPO {
    private Long id;
    private String fundCode;
    private Date navDate;
    private BigDecimal unitNav;
    private BigDecimal accumulatedNav;
    private BigDecimal dailyGrowthRate;
    private Date sourceAsOf;
    private Date fetchedAt;
    private Date createTime;
    private Date updateTime;
}
