package com.echoamoy.holdlens.server.domain.processing.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class USStockMarketRefreshDispatchCommandEntity {

    private String schemaVersion;
    private String serverTaskId;
    private Boolean allowNetwork;
    private String callbackUrl;

}
