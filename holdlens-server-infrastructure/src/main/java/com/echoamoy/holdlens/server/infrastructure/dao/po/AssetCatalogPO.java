package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetCatalogPO {
    private Long id;
    private Long userId;
    private Long parentId;
    private String catalogCode;
    private String catalogName;
    private String catalogScope;
    private String balanceDirection;
    private Integer sortOrder;
    private String status;
    private Date createTime;
    private Date updateTime;
}
