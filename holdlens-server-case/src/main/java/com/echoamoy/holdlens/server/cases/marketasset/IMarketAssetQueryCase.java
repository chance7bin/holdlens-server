package com.echoamoy.holdlens.server.cases.marketasset;

import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetQueryResult;

public interface IMarketAssetQueryCase {

    MarketAssetQueryResult.Watchlist queryWatchlist(Long userId, String assetKind);

    MarketAssetQueryResult.Search search(Long userId, String keyword, String assetKind, String market, Integer limit);

    MarketAssetQueryResult.StockDetail queryStockDetail(Long userId, String assetRef);
}
