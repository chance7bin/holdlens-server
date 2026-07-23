package com.echoamoy.holdlens.server.cases.marketasset;

import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetDetailResult;

public interface IMarketAssetDetailCase {
    MarketAssetDetailResult queryDetail(Long userId, String assetKind, String assetRef);
}
