package com.echoamoy.holdlens.server.domain.portfolio.service;

import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
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

    public AssetSummaryEntity summarize(List<AssetRecordEntity> records, String targetCurrency,
                                        Map<String, ExchangeRateEntity> ratesToCny) {
        String target = ExchangeRateEntity.normalizeCurrency(targetCurrency == null ? ExchangeRateEntity.CNY : targetCurrency);
        BigDecimal assetTotal = BigDecimal.ZERO;
        BigDecimal liabilityTotal = BigDecimal.ZERO;
        Set<String> missingCurrencies = new LinkedHashSet<>();
        Map<String, BigDecimal> unconverted = new LinkedHashMap<>();

        for (AssetRecordEntity record : records == null ? List.<AssetRecordEntity>of() : records) {
            BigDecimal converted = convert(record.getAmount(), record.getCurrency(), target, ratesToCny, missingCurrencies);
            if (converted == null) {
                unconverted.merge(record.getCurrency(), record.getAmount(), BigDecimal::add);
                continue;
            }
            if (AssetCatalogEntity.DIRECTION_SUBTRACT.equals(record.getBalanceDirection())) {
                liabilityTotal = liabilityTotal.add(converted);
            } else {
                assetTotal = assetTotal.add(converted);
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

    private BigDecimal convert(BigDecimal amount, String sourceCurrency, String targetCurrency,
                               Map<String, ExchangeRateEntity> ratesToCny, Set<String> missingCurrencies) {
        String source = ExchangeRateEntity.normalizeCurrency(sourceCurrency);
        if (source.equals(targetCurrency)) {
            return amount;
        }
        BigDecimal cnyAmount = amount;
        if (!ExchangeRateEntity.CNY.equals(source)) {
            ExchangeRateEntity sourceRate = ratesToCny.get(source);
            if (sourceRate == null) {
                missingCurrencies.add(source);
                return null;
            }
            cnyAmount = amount.multiply(sourceRate.getRate(), MathContext.DECIMAL128);
        }
        if (ExchangeRateEntity.CNY.equals(targetCurrency)) {
            return cnyAmount;
        }
        ExchangeRateEntity targetRate = ratesToCny.get(targetCurrency);
        if (targetRate == null) {
            missingCurrencies.add(targetCurrency);
            return null;
        }
        return cnyAmount.divide(targetRate.getRate(), MathContext.DECIMAL128);
    }
}
