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

    default List<AssetCatalogEntity> queryVisibleCatalogs(Long userId) { throw unsupported("queryVisibleCatalogs"); }

    default AssetCatalogEntity queryVisibleCatalog(Long userId, Long catalogId) { throw unsupported("queryVisibleCatalog"); }

    default AssetCatalogEntity queryCatalogByCode(String catalogCode) { throw unsupported("queryCatalogByCode"); }

    default int countEnabledChildren(Long userId, Long catalogId) { throw unsupported("countEnabledChildren"); }

    default int countActiveRecords(Long userId, Long catalogId) { throw unsupported("countActiveRecords"); }

    default void insertCatalog(AssetCatalogEntity catalog) { throw unsupported("insertCatalog"); }

    default void updateCatalog(AssetCatalogEntity catalog) { throw unsupported("updateCatalog"); }

    default void insertRecord(AssetRecordEntity record) { throw unsupported("insertRecord"); }

    default AssetRecordEntity queryRecord(Long userId, Long recordId) { throw unsupported("queryRecord"); }

    default AssetRecordEntity queryActiveRecord(Long userId, Long recordId) { throw unsupported("queryActiveRecord"); }

    default AssetRecordEntity queryRecordForUpdate(Long userId, Long recordId) { throw unsupported("queryRecordForUpdate"); }

    default List<AssetRecordEntity> queryActiveRecords(Long userId) { throw unsupported("queryActiveRecords"); }

    default List<AssetRecordEntity> queryActiveRecords(Long userId, String assetRef) {
        throw unsupported("queryActiveRecordsByAssetRef");
    }

    default void updateRecord(AssetRecordEntity record) { throw unsupported("updateRecord"); }

    default void insertRecordChanges(List<AssetRecordChangeEntity> changes) { throw unsupported("insertRecordChanges"); }

    default void upsertExchangeRate(ExchangeRateEntity rate) { throw unsupported("upsertExchangeRate"); }

    default ExchangeRateEntity queryExchangeRate(String baseCurrency, String quoteCurrency) { throw unsupported("queryExchangeRate"); }

    default List<ExchangeRateEntity> queryExchangeRates(Collection<String> baseCurrencies, String quoteCurrency) { throw unsupported("queryExchangeRates"); }

    List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId);

    void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets);

    default void deleteWatchlistAsset(Long userId, String assetKind, Long assetId) { throw unsupported("deleteWatchlistAsset"); }

    WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind);

    default WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind, String market) {
        throw unsupported("queryWatchlistAssetByMarket");
    }

    default List<WatchlistAssetEntity> queryWatchlistAssets(Long userId, String assetKind) { throw unsupported("queryWatchlistAssets"); }

    default Set<String> queryWatchlistedIdentityKeys(Long userId, Collection<String> identityKeys) { throw unsupported("queryWatchlistedIdentityKeys"); }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException("IPortfolioRepository must implement " + operation);
    }

}
