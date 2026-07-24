package com.echoamoy.holdlens.server.domain.portfolio.service;

import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetOverviewEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetSummaryEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.ExchangeRateEntity;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 当前资产汇总只使用用户确认的原币金额和最新汇率，不改写资产记录。
 */
public class AssetSummaryService {

    /**
     * 总览以同一份换算规则同时生成记录结果和目录递归小计，避免不同展示层产生金额偏差。
     */
    public AssetOverviewEntity summarizeOverview(List<AssetCatalogEntity> catalogs,
                                                  List<AssetRecordEntity> records,
                                                  String targetCurrency,
                                                  Map<String, ExchangeRateEntity> ratesToCny) {
        List<AssetCatalogEntity> safeCatalogs = catalogs == null ? List.of() : catalogs;
        List<AssetRecordEntity> safeRecords = records == null ? List.of() : records;
        String target = ExchangeRateEntity.normalizeCurrency(
                targetCurrency == null ? ExchangeRateEntity.CNY : targetCurrency);

        Map<Long, AssetCatalogEntity> catalogsById = new LinkedHashMap<>();
        Map<Long, CatalogAccumulator> catalogAmounts = new LinkedHashMap<>();
        for (AssetCatalogEntity catalog : safeCatalogs) {
            if (catalog.getId() == null) continue;
            catalogsById.put(catalog.getId(), catalog);
            catalogAmounts.put(catalog.getId(), new CatalogAccumulator());
        }

        List<AssetOverviewEntity.RecordAmount> recordAmounts = new ArrayList<>();
        for (AssetRecordEntity record : safeRecords) {
            Conversion conversion = convert(record.getAmount(), record.getCurrency(), target, ratesToCny);
            recordAmounts.add(AssetOverviewEntity.RecordAmount.builder()
                    .record(record)
                    .convertedAmount(conversion.amount)
                    .conversionStatus(conversion.amount == null
                            ? AssetOverviewEntity.CONVERSION_MISSING_RATE
                            : AssetOverviewEntity.CONVERSION_CONVERTED)
                    .build());
            accumulateCatalogAndAncestors(record.getCatalogId(), conversion, catalogsById, catalogAmounts);
        }

        List<AssetOverviewEntity.CatalogAmount> catalogResults = new ArrayList<>();
        for (AssetCatalogEntity catalog : safeCatalogs) {
            CatalogAccumulator accumulator = catalogAmounts.getOrDefault(catalog.getId(), new CatalogAccumulator());
            catalogResults.add(AssetOverviewEntity.CatalogAmount.builder()
                    .catalog(catalog)
                    .convertedAmount(accumulator.convertedAmount)
                    .partial(!accumulator.missingCurrencies.isEmpty())
                    .missingCurrencies(new ArrayList<>(accumulator.missingCurrencies))
                    .build());
        }

        return AssetOverviewEntity.builder()
                .summary(summarize(safeRecords, target, ratesToCny))
                .catalogs(catalogResults)
                .records(recordAmounts)
                .build();
    }

    public AssetSummaryEntity summarize(List<AssetRecordEntity> records, String targetCurrency,
                                        Map<String, ExchangeRateEntity> ratesToCny) {
        String target = ExchangeRateEntity.normalizeCurrency(targetCurrency == null ? ExchangeRateEntity.CNY : targetCurrency);
        BigDecimal assetTotal = BigDecimal.ZERO;
        BigDecimal liabilityTotal = BigDecimal.ZERO;
        Set<String> missingCurrencies = new LinkedHashSet<>();
        Map<String, BigDecimal> unconverted = new LinkedHashMap<>();

        for (AssetRecordEntity record : records == null ? List.<AssetRecordEntity>of() : records) {
            Conversion conversion = convert(record.getAmount(), record.getCurrency(), target, ratesToCny);
            if (conversion.amount == null) {
                missingCurrencies.addAll(conversion.missingCurrencies);
                unconverted.merge(record.getCurrency(), record.getAmount(), BigDecimal::add);
                continue;
            }
            if (AssetCatalogEntity.DIRECTION_SUBTRACT.equals(record.getBalanceDirection())) {
                liabilityTotal = liabilityTotal.add(conversion.amount);
            } else {
                assetTotal = assetTotal.add(conversion.amount);
            }
        }

        List<AssetSummaryEntity.UnconvertedAmount> unconvertedAmounts = new ArrayList<>();
        unconverted.forEach((currency, amount) -> unconvertedAmounts.add(
                AssetSummaryEntity.UnconvertedAmount.builder().currency(currency).amount(amount).build()));
        return AssetSummaryEntity.builder()
                .targetCurrency(target)
                .assetTotal(assetTotal)
                .liabilityTotal(liabilityTotal)
                .netAsset(assetTotal.subtract(liabilityTotal))
                .partial(!unconvertedAmounts.isEmpty())
                .missingCurrencies(new ArrayList<>(missingCurrencies))
                .unconvertedAmounts(unconvertedAmounts)
                .build();
    }

    private Conversion convert(BigDecimal amount, String sourceCurrency, String targetCurrency,
                               Map<String, ExchangeRateEntity> ratesToCny) {
        Set<String> missingCurrencies = new LinkedHashSet<>();
        String source = ExchangeRateEntity.normalizeCurrency(sourceCurrency);
        if (source.equals(targetCurrency)) {
            return new Conversion(amount, missingCurrencies);
        }
        ExchangeRateEntity sourceRate = ExchangeRateEntity.CNY.equals(source) ? null : ratesToCny.get(source);
        ExchangeRateEntity targetRate = ExchangeRateEntity.CNY.equals(targetCurrency)
                ? null : ratesToCny.get(targetCurrency);
        if (!ExchangeRateEntity.CNY.equals(source) && sourceRate == null) {
            missingCurrencies.add(source);
        }
        if (!ExchangeRateEntity.CNY.equals(targetCurrency) && targetRate == null) {
            missingCurrencies.add(targetCurrency);
        }
        if (!missingCurrencies.isEmpty()) {
            return new Conversion(null, missingCurrencies);
        }

        BigDecimal cnyAmount = ExchangeRateEntity.CNY.equals(source)
                ? amount : amount.multiply(sourceRate.getRate(), MathContext.DECIMAL128);
        if (ExchangeRateEntity.CNY.equals(targetCurrency)) {
            return new Conversion(cnyAmount, missingCurrencies);
        }
        return new Conversion(cnyAmount.divide(targetRate.getRate(), MathContext.DECIMAL128), missingCurrencies);
    }

    private void accumulateCatalogAndAncestors(Long catalogId, Conversion conversion,
                                               Map<Long, AssetCatalogEntity> catalogsById,
                                               Map<Long, CatalogAccumulator> catalogAmounts) {
        Long currentId = catalogId;
        Set<Long> visited = new LinkedHashSet<>();
        while (currentId != null && visited.add(currentId)) {
            CatalogAccumulator accumulator = catalogAmounts.get(currentId);
            AssetCatalogEntity catalog = catalogsById.get(currentId);
            if (accumulator == null || catalog == null) return;
            if (conversion.amount == null) {
                accumulator.missingCurrencies.addAll(conversion.missingCurrencies);
            } else {
                // 目录展示的是资产规模；负债方向只影响全局净资产，不把目录金额变为负数。
                accumulator.convertedAmount = accumulator.convertedAmount.add(conversion.amount);
            }
            currentId = catalog.getParentId();
        }
    }

    private static final class Conversion {
        private final BigDecimal amount;
        private final Set<String> missingCurrencies;

        private Conversion(BigDecimal amount, Set<String> missingCurrencies) {
            this.amount = amount;
            this.missingCurrencies = missingCurrencies;
        }
    }

    private static final class CatalogAccumulator {
        private BigDecimal convertedAmount = BigDecimal.ZERO;
        private final Set<String> missingCurrencies = new LinkedHashSet<>();
    }
}
