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
public class FundTopHoldingPO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long fundDetailItemId;
    private Long snapshotId;
    private Integer rankNo;
    private String stockName;
    private String stockCode;
    private String market;
    private BigDecimal dailyReturn;
    private BigDecimal holdingRatio;
    private String quarterChangeType;
    private BigDecimal quarterChangeValue;
    private String missingReasonsJson;
    private Date createTime;
    private Date updateTime;

}
