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
public class AssetHoldingChangePO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long holdingId;
    private Long accountId;
    private Long assetId;
    private String changeType;
    private BigDecimal beforeAmount;
    private BigDecimal afterAmount;
    private String currency;
    private String changeReason;
    private String sourceType;
    private String sourceRefId;
    private Long operatorId;
    private Date createTime;
    private Date updateTime;

}
