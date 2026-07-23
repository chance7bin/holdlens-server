package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.dto.AssetDTO;
import com.echoamoy.holdlens.server.api.request.AssetRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.portfolio.IAssetManagementCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.AssetManagementCommand;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetSummaryEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.ExchangeRateEntity;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

public class AssetControllerTest {

    @Test
    public void createRecordExposesOpaqueAssetRefWithoutPublicTechnicalId() throws Exception {
        AssetController controller = new AssetController(new FakeAssetCase());

        Response<AssetDTO.Record> response = controller.createRecord(AssetRequestDTO.CreateRecord.builder()
                .userId(1L).catalogId(5L).assetRef("fund:000001")
                .amount(new BigDecimal("1000")).currency("CNY").build());

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals("fund:000001", response.getData().getAssetRef());
        Assert.assertEquals("fund", response.getData().getAssetKind());
        Assert.assertThrows(NoSuchFieldException.class, () -> AssetDTO.Record.class.getDeclaredField("assetId"));
    }

    private static class FakeAssetCase implements IAssetManagementCase {
        @Override public List<AssetCatalogEntity> queryCatalogs(Long userId) { return List.of(); }
        @Override public AssetCatalogEntity createCatalog(AssetManagementCommand.CreateCatalog command) { return null; }
        @Override public AssetCatalogEntity updateCatalog(AssetManagementCommand.UpdateCatalog command) { return null; }
        @Override public void deleteCatalog(Long userId, Long catalogId) { }
        @Override public List<AssetRecordEntity> queryRecords(Long userId) { return List.of(); }
        @Override
        public AssetRecordEntity createRecord(AssetManagementCommand.CreateRecord command) {
            return AssetRecordEntity.builder().id(10L).catalogId(5L).catalogCode("FUND")
                    .recordName("示例基金").assetKind("FUND").assetId(88L).assetRef("fund:000001")
                    .amount(command.getAmount()).currency(command.getCurrency()).status("ACTIVE").build();
        }
        @Override public AssetRecordEntity updateRecordDetails(AssetManagementCommand.UpdateDetails command) { return null; }
        @Override public AssetRecordEntity updateRecordAmount(AssetManagementCommand.UpdateAmount command) { return null; }
        @Override public AssetRecordEntity archiveRecord(Long userId, Long recordId) { return null; }
        @Override public AssetRecordEntity restoreRecord(Long userId, Long recordId) { return null; }
        @Override public AssetRecordEntity deleteRecord(Long userId, Long recordId) { return null; }
        @Override public AssetRecordEntity splitRecord(AssetManagementCommand.SplitRecord command) { return null; }
        @Override public AssetSummaryEntity summarize(Long userId, String targetCurrency) { return null; }
        @Override public ExchangeRateEntity upsertExchangeRate(AssetManagementCommand.UpsertExchangeRate command) { return null; }
        @Override public List<ExchangeRateEntity> queryExchangeRates(List<String> baseCurrencies) { return List.of(); }
    }
}
