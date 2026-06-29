package com.echoamoy.holdlens.server.domain.portfolio.adapter.repository;

import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;

import java.util.List;

public interface IPortfolioRepository {

    List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId);

    void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets);

    WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind);

}
