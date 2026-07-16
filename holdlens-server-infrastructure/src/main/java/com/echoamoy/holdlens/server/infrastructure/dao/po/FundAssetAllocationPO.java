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
public class FundAssetAllocationPO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String fundCode;
    private String assetType;
    private String assetTypeName;
    private BigDecimal allocationRatio;
    private Integer displayOrder;
    private Date createTime;
    private Date updateTime;
}
