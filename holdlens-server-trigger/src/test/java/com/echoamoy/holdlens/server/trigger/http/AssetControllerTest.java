package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.dto.AssetDTO;
import com.echoamoy.holdlens.server.api.request.AssetRequestDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.portfolio.IAssetManagementCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.AssetManagementCommand;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetOverviewEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetSummaryEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.ExchangeRateEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
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

    @Test
    public void overviewExposesServerConvertedCatalogAndRecordAmounts() {
        AssetDTO.Overview overview = new AssetController(new FakeAssetCase()).overview(1L, "CNY").getData();

        Assert.assertEquals("CNY", overview.getTargetCurrency());
        Assert.assertEquals(0, new BigDecimal("100").compareTo(overview.getAssetTotal()));
        Assert.assertEquals(0, new BigDecimal("100").compareTo(overview.getCatalogs().get(0).getConvertedAmount()));
        Assert.assertFalse(overview.getCatalogs().get(0).isPartial());
        Assert.assertEquals(AssetOverviewEntity.CONVERSION_CONVERTED,
                overview.getRecords().get(0).getConversionStatus());
        Assert.assertEquals(0, new BigDecimal("100").compareTo(overview.getRecords().get(0).getConvertedAmount()));
        Assert.assertNotNull(overview.getRecords().get(0).getCreateTime());
    }

    @Test
    public void recordQueriesForwardOpaqueRefAndReturnActiveDetail() {
        FakeAssetCase fake = new FakeAssetCase();
        AssetController controller = new AssetController(fake);

        controller.queryRecords(1L, "fund:000001");
        AssetDTO.Record detail = controller.queryRecord(10L, 1L).getData();

        Assert.assertEquals("fund:000001", fake.queriedAssetRef);
        Assert.assertEquals(Long.valueOf(10L), detail.getId());
    }

    @Test
    public void requestParametersAndPathVariablesHaveExplicitRuntimeNames() {
        for (Method method : AssetController.class.getDeclaredMethods()) {
            for (Parameter parameter : method.getParameters()) {
                for (Annotation annotation : parameter.getAnnotations()) {
                    if (annotation instanceof RequestParam requestParam) {
                        Assert.assertFalse(method.getName(), requestParam.value().isBlank()
                                && requestParam.name().isBlank());
                    }
                    if (annotation instanceof PathVariable pathVariable) {
                        Assert.assertFalse(method.getName(), pathVariable.value().isBlank()
                                && pathVariable.name().isBlank());
                    }
                }
            }
        }
    }

    private static class FakeAssetCase implements IAssetManagementCase {
        private String queriedAssetRef;
        @Override public List<AssetCatalogEntity> queryCatalogs(Long userId) { return List.of(); }
        @Override public AssetCatalogEntity createCatalog(AssetManagementCommand.CreateCatalog command) { return null; }
        @Override public AssetCatalogEntity updateCatalog(AssetManagementCommand.UpdateCatalog command) { return null; }
        @Override public void deleteCatalog(Long userId, Long catalogId) { }
        @Override public List<AssetRecordEntity> queryRecords(Long userId) { return List.of(); }
        @Override public List<AssetRecordEntity> queryRecords(Long userId, String assetRef) {
            queriedAssetRef = assetRef;
            return List.of();
        }
        @Override public AssetRecordEntity queryRecord(Long userId, Long recordId) { return fundRecord(); }
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
        @Override
        public AssetOverviewEntity overview(Long userId, String targetCurrency) {
            AssetCatalogEntity catalog = AssetCatalogEntity.builder().id(5L).catalogCode("FUND")
                    .catalogName("基金").catalogScope("SYSTEM").balanceDirection("ADD")
                    .sortOrder(1).status("ENABLED").build();
            AssetRecordEntity record = fundRecord();
            return AssetOverviewEntity.builder()
                    .summary(AssetSummaryEntity.builder().targetCurrency("CNY").assetTotal(new BigDecimal("100"))
                            .liabilityTotal(BigDecimal.ZERO).netAsset(new BigDecimal("100")).partial(false)
                            .missingCurrencies(List.of()).unconvertedAmounts(List.of()).build())
                    .catalogs(List.of(AssetOverviewEntity.CatalogAmount.builder().catalog(catalog)
                            .convertedAmount(new BigDecimal("100")).partial(false).missingCurrencies(List.of()).build()))
                    .records(List.of(AssetOverviewEntity.RecordAmount.builder().record(record)
                            .convertedAmount(new BigDecimal("100"))
                            .conversionStatus(AssetOverviewEntity.CONVERSION_CONVERTED).build()))
                    .build();
        }
        @Override public ExchangeRateEntity upsertExchangeRate(AssetManagementCommand.UpsertExchangeRate command) { return null; }
        @Override public List<ExchangeRateEntity> queryExchangeRates(List<String> baseCurrencies) { return List.of(); }

        private AssetRecordEntity fundRecord() {
            return AssetRecordEntity.builder().id(10L).catalogId(5L).catalogCode("FUND")
                    .recordName("示例基金").assetKind("FUND").assetId(88L).assetRef("fund:000001")
                    .assetCode("000001").amount(new BigDecimal("100")).currency("CNY").status("ACTIVE")
                    .createTime(LocalDateTime.of(2026, 7, 23, 10, 0))
                    .updateTime(LocalDateTime.of(2026, 7, 23, 10, 1)).build();
        }
    }
}
