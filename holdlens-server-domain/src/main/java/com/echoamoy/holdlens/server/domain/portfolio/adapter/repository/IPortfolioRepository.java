package com.echoamoy.holdlens.server.domain.portfolio.adapter.repository;

import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordChangeEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.ExchangeRateEntity;

import java.util.List;
import java.util.Collection;
import java.util.Set;

public interface IPortfolioRepository {

    default List<AssetCatalogEntity> queryVisibleCatalogs(Long userId) { return List.of(); }

    default AssetCatalogEntity queryVisibleCatalog(Long userId, Long catalogId) { return null; }

    default AssetCatalogEntity queryCatalogByCode(String catalogCode) { return null; }

    default int countEnabledChildren(Long userId, Long catalogId) { return 0; }

    default int countActiveRecords(Long userId, Long catalogId) { return 0; }

    default void insertCatalog(AssetCatalogEntity catalog) { throw new UnsupportedOperationException(); }

    default void updateCatalog(AssetCatalogEntity catalog) { throw new UnsupportedOperationException(); }

    default void insertRecord(AssetRecordEntity record) { throw new UnsupportedOperationException(); }

    default AssetRecordEntity queryRecord(Long userId, Long recordId) { return null; }

    default AssetRecordEntity queryActiveRecord(Long userId, Long recordId) { return null; }

    default AssetRecordEntity queryRecordForUpdate(Long userId, Long recordId) { return queryRecord(userId, recordId); }

    default List<AssetRecordEntity> queryActiveRecords(Long userId) { return List.of(); }

    default List<AssetRecordEntity> queryActiveRecords(Long userId, String assetRef) {
        return queryActiveRecords(userId);
    }

    default void updateRecord(AssetRecordEntity record) { throw new UnsupportedOperationException(); }

    default void insertRecordChanges(List<AssetRecordChangeEntity> changes) { throw new UnsupportedOperationException(); }

    default void upsertExchangeRate(ExchangeRateEntity rate) { throw new UnsupportedOperationException(); }

    default ExchangeRateEntity queryExchangeRate(String baseCurrency, String quoteCurrency) { return null; }

    default List<ExchangeRateEntity> queryExchangeRates(Collection<String> baseCurrencies, String quoteCurrency) { return List.of(); }

    List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId);

    void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets);

    default void deleteWatchlistAsset(Long userId, String assetKind, Long assetId) { }

    WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind);

    default WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind, String market) {
        return queryWatchlistAsset(userId, assetCode, assetKind);
    }

    default List<WatchlistAssetEntity> queryWatchlistAssets(Long userId, String assetKind) { return List.of(); }

    default Set<String> queryWatchlistedIdentityKeys(Long userId, Collection<String> identityKeys) { return Set.of(); }

}
