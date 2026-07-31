package com.echoamoy.holdlens.server.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketAssetDetailEnsureRequest {
    private Long userId;
    private String assetKind;
    private String assetRef;
}
