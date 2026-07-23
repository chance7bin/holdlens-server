package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.dto.FundDetailDTO;
import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;

import java.util.List;

final class FundDetailDtoMapper {

    private FundDetailDtoMapper() { }

    static FundDetailDTO toDTO(PortfolioFundDetailResult.FundDetail detail) {
        if (detail == null) return null;
        return FundDetailDTO.builder()
                .fundCode(detail.getFundCode()).fundName(detail.getFundName()).fundType(detail.getFundType())
                .detailStatus(detail.getDetailStatus()).buyStatus(detail.getBuyStatus())
                .dailyPurchaseLimit(detail.getDailyPurchaseLimit()).returnsAsOf(detail.getReturnsAsOf())
                .unitNav(detail.getUnitNav()).accumulatedNav(detail.getAccumulatedNav())
                .dailyGrowthRate(detail.getDailyGrowthRate()).returnCoverageStatus(detail.getReturnCoverageStatus())
                .topHoldingsAsOf(detail.getTopHoldingsAsOf()).publicHoldingsStatus(detail.getPublicHoldingsStatus())
                .assetAllocationAsOf(detail.getAssetAllocationAsOf())
                .assetAllocationStatus(detail.getAssetAllocationStatus())
                .oneMonthReturn(detail.getOneMonthReturn()).threeMonthsReturn(detail.getThreeMonthsReturn())
                .sixMonthsReturn(detail.getSixMonthsReturn()).oneYearReturn(detail.getOneYearReturn())
                .threeYearsReturn(detail.getThreeYearsReturn()).catalogFetchedAt(detail.getCatalogFetchedAt())
                .purchaseStatusFetchedAt(detail.getPurchaseStatusFetchedAt())
                .periodReturnFetchedAt(detail.getPeriodReturnFetchedAt())
                .topHoldingFetchedAt(detail.getTopHoldingFetchedAt())
                .assetAllocationFetchedAt(detail.getAssetAllocationFetchedAt())
                .topHoldingRefreshStatus(detail.getTopHoldingRefreshStatus())
                .topHoldings(toTopHoldings(detail.getTopHoldings()))
                .assetAllocations(toAssetAllocations(detail.getAssetAllocations())).build();
    }

    private static List<FundDetailDTO.TopHolding> toTopHoldings(
            List<PortfolioFundDetailResult.TopHolding> holdings) {
        if (holdings == null) return null;
        return holdings.stream().map(item -> item == null ? null : FundDetailDTO.TopHolding.builder()
                .rankNo(item.getRankNo()).stockName(item.getStockName()).stockCode(item.getStockCode())
                .market(item.getMarket()).changePercent(item.getChangePercent()).refreshedAt(item.getRefreshedAt())
                .quoteStatus(item.getQuoteStatus()).holdingRatio(item.getHoldingRatio())
                .quarterChangeType(item.getQuarterChangeType()).quarterChangeValue(item.getQuarterChangeValue()).build())
                .toList();
    }

    private static List<FundDetailDTO.AssetAllocation> toAssetAllocations(
            List<PortfolioFundDetailResult.AssetAllocation> allocations) {
        if (allocations == null) return null;
        return allocations.stream().map(item -> item == null ? null : FundDetailDTO.AssetAllocation.builder()
                .assetType(item.getAssetType()).assetTypeName(item.getAssetTypeName())
                .allocationRatio(item.getAllocationRatio()).displayOrder(item.getDisplayOrder()).build()).toList();
    }
}
