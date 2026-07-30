package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FundPeriodPerformancePO {
    private Long id;
    private String fundCode;
    private String period;
    private BigDecimal fundReturn;
    private BigDecimal peerAverage;
    private Integer peerRank;
    private Integer peerTotal;
    private Integer rankChange;
    private Date asOf;
    private Date fetchedAt;
    private Date createTime;
    private Date updateTime;
}
