package com.echoamoy.holdlens.server.domain.marketdetail.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MarketDetailDispatchCommandEntity {
    private String schemaVersion;
    private String serverTaskId;
    private String assetKind;
    private String assetRef;
    private String providerMarketCode;
    private List<String> slices;
    private List<String> periods;
    private String callbackUrl;
    private Boolean allowNetwork;
    private String requestedAt;
}
