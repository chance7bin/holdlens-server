package com.echoamoy.holdlens.server.cases.portfolio;

import com.echoamoy.holdlens.server.cases.portfolio.model.AssetManagementCommand;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetSummaryEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.ExchangeRateEntity;

import java.util.List;

public interface IAssetManagementCase {

    List<AssetCatalogEntity> queryCatalogs(Long userId);

    AssetCatalogEntity createCatalog(AssetManagementCommand.CreateCatalog command);

    AssetCatalogEntity updateCatalog(AssetManagementCommand.UpdateCatalog command);

    void deleteCatalog(Long userId, Long catalogId);

    List<AssetRecordEntity> queryRecords(Long userId);

    AssetRecordEntity createRecord(AssetManagementCommand.CreateRecord command);

    AssetRecordEntity updateRecordDetails(AssetManagementCommand.UpdateDetails command);

    AssetRecordEntity updateRecordAmount(AssetManagementCommand.UpdateAmount command);

    AssetRecordEntity archiveRecord(Long userId, Long recordId);

    AssetRecordEntity restoreRecord(Long userId, Long recordId);

    AssetRecordEntity deleteRecord(Long userId, Long recordId);

    AssetRecordEntity splitRecord(AssetManagementCommand.SplitRecord command);

    AssetSummaryEntity summarize(Long userId, String targetCurrency);

    ExchangeRateEntity upsertExchangeRate(AssetManagementCommand.UpsertExchangeRate command);

    List<ExchangeRateEntity> queryExchangeRates(List<String> baseCurrencies);
}
