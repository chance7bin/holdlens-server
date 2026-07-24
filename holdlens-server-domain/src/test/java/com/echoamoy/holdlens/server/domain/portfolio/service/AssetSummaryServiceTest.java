package com.echoamoy.holdlens.server.domain.portfolio.service;

import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetOverviewEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetSummaryEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.ExchangeRateEntity;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AssetSummaryServiceTest {

    private final AssetSummaryService service = new AssetSummaryService();

    @Test
    public void convertsForeignCurrenciesThroughCnyAndAppliesDirections() {
        AssetRecordEntity usdAsset = record("100", "USD", AssetCatalogEntity.DIRECTION_ADD);
        AssetRecordEntity hkdLiability = record("100", "HKD", AssetCatalogEntity.DIRECTION_SUBTRACT);
        Map<String, ExchangeRateEntity> rates = Map.of(
                "USD", rate("USD", "7.2"),
                "HKD", rate("HKD", "0.9"));

        AssetSummaryEntity summary = service.summarize(List.of(usdAsset, hkdLiability), "USD", rates);

        Assert.assertEquals(0, new BigDecimal("100").compareTo(summary.getAssetTotal()));
        Assert.assertEquals(0, new BigDecimal("12.5").compareTo(summary.getLiabilityTotal()));
        Assert.assertEquals(0, new BigDecimal("87.5").compareTo(summary.getNetAsset()));
        Assert.assertFalse(summary.isPartial());
    }

    @Test
    public void missingRateReturnsUnconvertedOriginalAmount() {
        AssetSummaryEntity summary = service.summarize(
                List.of(record("100", "EUR", AssetCatalogEntity.DIRECTION_ADD)), "CNY", Map.of());

        Assert.assertTrue(summary.isPartial());
        Assert.assertEquals(BigDecimal.ZERO, summary.getAssetTotal());
        Assert.assertEquals(List.of("EUR"), summary.getMissingCurrencies());
        Assert.assertEquals(0, new BigDecimal("100").compareTo(summary.getUnconvertedAmounts().get(0).getAmount()));
    }

    @Test
    public void overviewRecursivelyAggregatesCatalogsAndKeepsLiabilityMagnitudePositive() {
        AssetCatalogEntity root = catalog(1L, null, AssetCatalogEntity.DIRECTION_SUBTRACT);
        AssetCatalogEntity leaf = catalog(2L, 1L, AssetCatalogEntity.DIRECTION_SUBTRACT);
        AssetRecordEntity liability = withCatalog(record("100", "USD", AssetCatalogEntity.DIRECTION_SUBTRACT), 2L);

        AssetOverviewEntity overview = service.summarizeOverview(
                List.of(root, leaf), List.of(liability), "CNY", Map.of("USD", rate("USD", "7.2")));

        Assert.assertEquals(0, new BigDecimal("720").compareTo(overview.getCatalogs().get(0).getConvertedAmount()));
        Assert.assertEquals(0, new BigDecimal("720").compareTo(overview.getCatalogs().get(1).getConvertedAmount()));
        Assert.assertEquals(0, new BigDecimal("720").compareTo(overview.getSummary().getLiabilityTotal()));
        Assert.assertEquals(0, new BigDecimal("-720").compareTo(overview.getSummary().getNetAsset()));
        Assert.assertEquals(AssetOverviewEntity.CONVERSION_CONVERTED,
                overview.getRecords().get(0).getConversionStatus());
    }

    @Test
    public void overviewPropagatesMissingRateToLeafAndAncestor() {
        AssetCatalogEntity root = catalog(1L, null, AssetCatalogEntity.DIRECTION_ADD);
        AssetCatalogEntity leaf = catalog(2L, 1L, AssetCatalogEntity.DIRECTION_ADD);
        AssetRecordEntity cny = withCatalog(record("100", "CNY", AssetCatalogEntity.DIRECTION_ADD), 2L);
        AssetRecordEntity eur = withCatalog(record("20", "EUR", AssetCatalogEntity.DIRECTION_ADD), 2L);

        AssetOverviewEntity overview = service.summarizeOverview(
                List.of(root, leaf), List.of(cny, eur), "CNY", Map.of());

        for (AssetOverviewEntity.CatalogAmount catalog : overview.getCatalogs()) {
            Assert.assertEquals(0, new BigDecimal("100").compareTo(catalog.getConvertedAmount()));
            Assert.assertTrue(catalog.isPartial());
            Assert.assertEquals(List.of("EUR"), catalog.getMissingCurrencies());
        }
        Assert.assertEquals(AssetOverviewEntity.CONVERSION_MISSING_RATE,
                overview.getRecords().get(1).getConversionStatus());
        Assert.assertNull(overview.getRecords().get(1).getConvertedAmount());
    }

    @Test
    public void reportsEveryMissingRateRequiredForCrossCurrencyConversion() {
        AssetSummaryEntity summary = service.summarize(
                List.of(record("20", "EUR", AssetCatalogEntity.DIRECTION_ADD)), "USD", Map.of());

        Assert.assertEquals(List.of("EUR", "USD"), summary.getMissingCurrencies());
        Assert.assertTrue(summary.isPartial());
    }

    private AssetRecordEntity record(String amount, String currency, String direction) {
        AssetRecordEntity record = AssetRecordEntity.create(1L, 2L, "记录", null, null,
                new BigDecimal(amount), currency, null);
        record.attachCatalogContext(null, direction);
        return record;
    }

    private ExchangeRateEntity rate(String base, String value) {
        return ExchangeRateEntity.current(base, "CNY", new BigDecimal(value), null, null, null);
    }

    private AssetCatalogEntity catalog(Long id, Long parentId, String direction) {
        return AssetCatalogEntity.builder().id(id).parentId(parentId).catalogName("目录" + id)
                .catalogScope(AssetCatalogEntity.SCOPE_SYSTEM).balanceDirection(direction)
                .status(AssetCatalogEntity.STATUS_ENABLED).build();
    }

    private AssetRecordEntity withCatalog(AssetRecordEntity source, Long catalogId) {
        return AssetRecordEntity.builder().id(source.getId()).userId(source.getUserId()).catalogId(catalogId)
                .catalogCode(source.getCatalogCode()).balanceDirection(source.getBalanceDirection())
                .recordName(source.getRecordName()).assetKind(source.getAssetKind()).assetId(source.getAssetId())
                .amount(source.getAmount()).currency(source.getCurrency()).remark(source.getRemark())
                .status(source.getStatus()).build();
    }
}
