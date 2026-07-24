package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.dto.AssetDTO;
import com.echoamoy.holdlens.server.api.request.AssetRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;

import java.util.List;

public interface IAssetService {

    Response<List<AssetDTO.Catalog>> queryCatalogs(Long userId);

    Response<AssetDTO.Catalog> createCatalog(AssetRequestDTO.CreateCatalog request);

    Response<AssetDTO.Catalog> updateCatalog(Long catalogId, AssetRequestDTO.UpdateCatalog request);

    Response<Void> deleteCatalog(Long catalogId, AssetRequestDTO.UserOperation request);

    Response<List<AssetDTO.Record>> queryRecords(Long userId, String assetRef);

    Response<AssetDTO.Record> queryRecord(Long recordId, Long userId);

    Response<AssetDTO.Record> createRecord(AssetRequestDTO.CreateRecord request);

    Response<AssetDTO.Record> updateRecordDetails(Long recordId, AssetRequestDTO.UpdateDetails request);

    Response<AssetDTO.Record> updateRecordAmount(Long recordId, AssetRequestDTO.UpdateAmount request);

    Response<AssetDTO.Record> changeRecordStatus(Long recordId, String action, AssetRequestDTO.UserOperation request);

    Response<AssetDTO.Record> splitRecord(Long recordId, AssetRequestDTO.SplitRecord request);

    Response<AssetDTO.Summary> summarize(Long userId, String targetCurrency);

    Response<AssetDTO.Overview> overview(Long userId, String targetCurrency);

    Response<List<AssetDTO.ExchangeRate>> queryExchangeRates(List<String> baseCurrencies);

    Response<AssetDTO.ExchangeRate> upsertExchangeRate(AssetRequestDTO.UpsertExchangeRate request);
}
