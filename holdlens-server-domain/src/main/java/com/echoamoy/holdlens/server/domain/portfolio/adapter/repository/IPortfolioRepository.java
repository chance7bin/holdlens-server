package com.echoamoy.holdlens.server.domain.portfolio.adapter.repository;

import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;

import java.util.List;
import java.util.Collection;
import java.util.Set;

public interface IPortfolioRepository {

    List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId);

    void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets);

    WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind);

    default List<WatchlistAssetEntity> queryWatchlistAssets(Long userId, String assetKind) { return List.of(); }

    default Set<String> queryWatchlistedIdentityKeys(Long userId, Collection<String> identityKeys) { return Set.of(); }

}
