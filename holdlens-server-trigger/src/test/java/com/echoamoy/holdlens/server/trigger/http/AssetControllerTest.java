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
        Assert.assertEquals("000001", response.getData().getFund().getAssetCode());
        Assert.assertNull(response.getData().getStock());
        Assert.assertThrows(NoSuchFieldException.class, () -> AssetDTO.Record.class.getDeclaredField("assetId"));
    }

    @Test
    public void createStockRecordExposesOnlyStockIdentity() {
        AssetController controller = new AssetController(new FakeAssetCase());

        AssetDTO.Record record = controller.createRecord(AssetRequestDTO.CreateRecord.builder()
                .userId(1L).catalogId(6L).assetRef("stock:US_STOCK:DEMO")
                .amount(BigDecimal.TEN).currency("USD").build()).getData();

        Assert.assertNull(record.getFund());
        Assert.assertEquals("DEMO", record.getStock().getAssetCode());
        Assert.assertEquals("US_STOCK", record.getStock().getAssetMarket());
        Assert.assertEquals("美股", record.getStock().getAssetMarketLabel());
    }

    private static class FakeAssetCase implements IAssetManagementCase {
        @Override public List<AssetCatalogEntity> queryCatalogs(Long userId) { return List.of(); }
        @Override public AssetCatalogEntity createCatalog(AssetManagementCommand.CreateCatalog command) { return null; }
        @Override public AssetCatalogEntity updateCatalog(AssetManagementCommand.UpdateCatalog command) { return null; }
        @Override public void deleteCatalog(Long userId, Long catalogId) { }
        @Override public List<AssetRecordEntity> queryRecords(Long userId) { return List.of(); }
        @Override
        public AssetRecordEntity createRecord(AssetManagementCommand.CreateRecord command) {
            if (command.getAssetRef() != null && command.getAssetRef().startsWith("stock:")) {
                return AssetRecordEntity.builder().id(11L).catalogId(6L).catalogCode("STOCK")
                        .recordName("示例股票").assetKind("STOCK").assetId(99L)
                        .assetRef("stock:US_STOCK:DEMO").assetCode("DEMO").assetMarket("US_STOCK")
                        .amount(command.getAmount()).currency(command.getCurrency()).status("ACTIVE").build();
            }
            return AssetRecordEntity.builder().id(10L).catalogId(5L).catalogCode("FUND")
                    .recordName("示例基金").assetKind("FUND").assetId(88L).assetRef("fund:000001")
                    .assetCode("000001")
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
