package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRecordPO {
    private Long id;
    private Long userId;
    private Long catalogId;
    private String catalogCode;
    private String balanceDirection;
    private String recordName;
    private String assetKind;
    private Long assetId;
    private String assetRef;
    private BigDecimal amount;
    private String currency;
    private String remark;
    private String status;
    private Date createTime;
    private Date updateTime;
}
