package com.echoamoy.holdlens.server.domain.marketdetail.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDetailSliceStateEntity {
    private Long id;
    private String assetRef;
    private String sliceType;
    private String status;
    private String activeTaskId;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime lastSuccessAt;
    private String errorSummary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
