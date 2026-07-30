package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MarketDetailSliceStatePO {
    private Long id;
    private String fundCode;
    private String sliceType;
    private String status;
    private String activeTaskId;
    private Date lastAttemptAt;
    private Date lastSuccessAt;
    private String errorSummary;
    private Date createTime;
    private Date updateTime;
}
