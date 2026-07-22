package com.echoamoy.holdlens.server.infrastructure.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MarketDetailDispatchRequestDTO {
    @JsonProperty("schema_version") private String schemaVersion;
    @JsonProperty("server_task_id") private String serverTaskId;
    @JsonProperty("asset_kind") private String assetKind;
    @JsonProperty("asset_ref") private String assetRef;
    @JsonProperty("provider_market_code") private String providerMarketCode;
    private List<String> slices;
    private List<String> periods;
    @JsonProperty("callback_url") private String callbackUrl;
    @JsonProperty("allow_network") private Boolean allowNetwork;
    @JsonProperty("requested_at") private String requestedAt;
}
