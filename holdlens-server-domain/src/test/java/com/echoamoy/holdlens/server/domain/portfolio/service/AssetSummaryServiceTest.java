package com.echoamoy.holdlens.server.domain.portfolio.service;

import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
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

    private AssetRecordEntity record(String amount, String currency, String direction) {
        AssetRecordEntity record = AssetRecordEntity.create(1L, 2L, "记录", null, null,
                new BigDecimal(amount), currency, null);
        record.attachCatalogContext(null, direction);
        return record;
    }

    private ExchangeRateEntity rate(String base, String value) {
        return ExchangeRateEntity.current(base, "CNY", new BigDecimal(value), null, null, null);
    }
}
