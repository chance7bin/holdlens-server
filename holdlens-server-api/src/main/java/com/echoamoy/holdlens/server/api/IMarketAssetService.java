package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.dto.MarketAssetDTO;
import com.echoamoy.holdlens.server.api.response.Response;

public interface IMarketAssetService {

    Response<MarketAssetDTO.Watchlist> queryWatchlist(Long userId, String assetKind);

    Response<MarketAssetDTO.Search> search(Long userId, String keyword, String assetKind, String market, Integer limit);

    Response<MarketAssetDTO.StockDetail> queryStockDetail(Long userId, String assetRef);
}
