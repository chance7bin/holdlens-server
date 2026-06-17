package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.IPortfolioFundDetailCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundDetailSnapshotAggregate;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PortfolioFundDetailCaseImpl implements IPortfolioFundDetailCase {

    private static final int STALE_DAYS = 7;

    @Resource
    private IPortfolioRepository portfolioRepository;

    @Resource
    private IFundDataRepository fundDataRepository;

    @Override
    public PortfolioFundDetailResult queryPortfolioFundDetails(Long userId) {
        if (userId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户标识不能为空");
        }

        List<PortfolioHoldingEntity> holdings = portfolioRepository.queryCurrentHoldings(userId);
        Set<String> fundCodes = new LinkedHashSet<>();
        for (PortfolioHoldingEntity holding : holdings) {
            String fundCode = holding.fundCodeOrNull();
            if (fundCode != null) {
                fundCodes.add(fundCode);
            }
        }
        Map<String, FundDetailSnapshotAggregate.FundDetail> latestDetails = fundDataRepository.queryLatestDetails(fundCodes);
        return PortfolioFundDetailResult.builder()
                .userId(userId)
                .holdings(holdings.stream()
                        .map(holding -> toHoldingDetail(holding, latestDetails.get(holding.fundCodeOrNull())))
                        .toList())
                .build();
    }

    private PortfolioFundDetailResult.HoldingDetail toHoldingDetail(PortfolioHoldingEntity holding,
                                                                    FundDetailSnapshotAggregate.FundDetail fundDetail) {
        return PortfolioFundDetailResult.HoldingDetail.builder()
                .holdingId(holding.getHoldingId())
                .accountId(holding.getAccountId())
                .accountName(holding.getAccountName())
                .accountType(holding.getAccountType())
                .assetId(holding.getAssetId())
                .assetCode(holding.getAssetCode())
                .assetName(holding.getAssetName())
                .assetKind(holding.getAssetKind())
                .assetType(holding.getAssetType())
                .assetCategory(holding.getAssetCategory())
                .holdingSource(holding.getHoldingSource())
                .amount(holding.getAmount())
                .currency(holding.getCurrency())
                .amountDisplay(holding.getAmountDisplay())
                .amountMissingReason(holding.getAmountMissingReason())
                .status(holding.getStatus())
                .fundDetail(toFundDetail(holding.fundCodeOrNull(), fundDetail))
                .build();
    }

    private PortfolioFundDetailResult.FundDetail toFundDetail(String fundCode, FundDetailSnapshotAggregate.FundDetail detail) {
        if (fundCode == null) {
            return PortfolioFundDetailResult.FundDetail.builder().detailStatus("unavailable").build();
        }
        if (detail == null) {
            return PortfolioFundDetailResult.FundDetail.builder()
                    .fundCode(fundCode)
                    .detailStatus("missing")
                    .build();
        }
        return PortfolioFundDetailResult.FundDetail.builder()
                .fundCode(detail.getFundCode())
                .fundName(detail.getFundName())
                .detailStatus(isStale(detail) ? "stale" : "available")
                .buyStatus(detail.getBuyStatus())
                .dailyPurchaseLimit(detail.getDailyPurchaseLimit())
                .generatedAt(detail.getGeneratedAt())
                .returnsAsOf(detail.getReturnsAsOf())
                .topHoldingsAsOf(detail.getTopHoldingsAsOf())
                .publicHoldingsStatus(detail.getPublicHoldingsStatus())
                .oneMonthReturn(detail.getOneMonthReturn())
                .threeMonthsReturn(detail.getThreeMonthsReturn())
                .sixMonthsReturn(detail.getSixMonthsReturn())
                .oneYearReturn(detail.getOneYearReturn())
                .threeYearsReturn(detail.getThreeYearsReturn())
                .fieldSourcesJson(detail.getFieldSourcesJson())
                .missingReasonsJson(detail.getMissingReasonsJson())
                .topHoldings(detail.getTopHoldings() == null ? List.of() : detail.getTopHoldings().stream()
                        .map(this::toTopHolding)
                        .toList())
                .build();
    }

    private PortfolioFundDetailResult.TopHolding toTopHolding(FundDetailSnapshotAggregate.TopHolding topHolding) {
        return PortfolioFundDetailResult.TopHolding.builder()
                .rankNo(topHolding.getRankNo())
                .stockName(topHolding.getStockName())
                .stockCode(topHolding.getStockCode())
                .market(topHolding.getMarket())
                .dailyReturn(topHolding.getDailyReturn())
                .holdingRatio(topHolding.getHoldingRatio())
                .quarterChangeType(topHolding.getQuarterChangeType())
                .quarterChangeValue(topHolding.getQuarterChangeValue())
                .missingReasonsJson(topHolding.getMissingReasonsJson())
                .build();
    }

    private boolean isStale(FundDetailSnapshotAggregate.FundDetail detail) {
        return detail.getGeneratedAt() != null
                && detail.getGeneratedAt().toInstant().isBefore(Instant.now().minus(STALE_DAYS, ChronoUnit.DAYS));
    }

}
