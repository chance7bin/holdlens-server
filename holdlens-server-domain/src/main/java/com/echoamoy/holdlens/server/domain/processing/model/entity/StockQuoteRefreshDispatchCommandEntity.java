package com.echoamoy.holdlens.server.domain.processing.model.entity;

import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteTargetEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuoteRefreshDispatchCommandEntity {

    private String schemaVersion;
    private String serverTaskId;
    private List<StockQuoteTargetEntity> stocks;
    private Boolean allowNetwork;
    private String callbackUrl;

}
