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
public class AssetHoldingPO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long accountId;
    private Long assetId;
    private String assetCategory;
    private String holdingSource;
    private BigDecimal amount;
    private String currency;
    private String amountDisplay;
    private String amountMissingReason;
    private String missingReasonsJson;
    private String status;
    private Date createTime;
    private Date updateTime;

}
