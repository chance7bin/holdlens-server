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
public class AssetRecordChangePO {
    private Long id;
    private String operationId;
    private Long userId;
    private Long recordId;
    private String changeType;
    private BigDecimal beforeAmount;
    private BigDecimal afterAmount;
    private String currency;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private Date createTime;
}
