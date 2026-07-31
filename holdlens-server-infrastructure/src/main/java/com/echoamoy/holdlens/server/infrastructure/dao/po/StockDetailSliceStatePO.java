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
public class StockDetailSliceStatePO {
    private Long id;
    private String assetRef;
    private String sliceType;
    private String status;
    private String activeTaskId;
    private Date lastAttemptAt;
    private Date lastSuccessAt;
    private String errorSummary;
    private Date createTime;
    private Date updateTime;
}
