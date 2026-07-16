package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IPortfolioFundDetailService;
import com.echoamoy.holdlens.server.api.dto.PortfolioFundDetailDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.portfolio.IPortfolioFundDetailCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
public class PortfolioFundDetailController implements IPortfolioFundDetailService {

    @Resource
    private IPortfolioFundDetailCase portfolioFundDetailCase;

    @GetMapping("/api/portfolio/assets/fund-details")
    @Override
    public Response<PortfolioFundDetailDTO> queryPortfolioFundDetails(@RequestParam Long userId) {
        return Response.ok(toDTO(portfolioFundDetailCase.queryPortfolioFundDetails(userId)));
    }

    @GetMapping("/api/funds/{fundCode}")
    @Override
    public Response<PortfolioFundDetailDTO.FundDetail> queryFundDetail(@PathVariable String fundCode) {
        return Response.ok(toFundDetail(portfolioFundDetailCase.queryFundDetail(fundCode)));
    }

    private PortfolioFundDetailDTO toDTO(PortfolioFundDetailResult result) {
        if (result == null) {
            return null;
        }
        return PortfolioFundDetailDTO.builder()
                .userId(result.getUserId())
                .holdings(toHoldingDetails(result.getHoldings()))
                .build();
    }

    private List<PortfolioFundDetailDTO.HoldingDetail> toHoldingDetails(List<PortfolioFundDetailResult.HoldingDetail> holdings) {
        if (holdings == null) {
            return null;
        }
        return holdings.stream()
                .map(holding -> holding == null ? null : PortfolioFundDetailDTO.HoldingDetail.builder()
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
                        .fundDetail(toFundDetail(holding.getFundDetail()))
                        .build())
                .toList();
    }

    private PortfolioFundDetailDTO.FundDetail toFundDetail(PortfolioFundDetailResult.FundDetail fundDetail) {
        if (fundDetail == null) {
            return null;
        }
        return PortfolioFundDetailDTO.FundDetail.builder()
                .fundCode(fundDetail.getFundCode())
                .fundName(fundDetail.getFundName())
                .fundType(fundDetail.getFundType())
                .detailStatus(fundDetail.getDetailStatus())
                .buyStatus(fundDetail.getBuyStatus())
                .dailyPurchaseLimit(fundDetail.getDailyPurchaseLimit())
                .returnsAsOf(fundDetail.getReturnsAsOf())
                .unitNav(fundDetail.getUnitNav())
                .accumulatedNav(fundDetail.getAccumulatedNav())
                .dailyGrowthRate(fundDetail.getDailyGrowthRate())
                .returnCoverageStatus(fundDetail.getReturnCoverageStatus())
                .topHoldingsAsOf(fundDetail.getTopHoldingsAsOf())
                .publicHoldingsStatus(fundDetail.getPublicHoldingsStatus())
                .assetAllocationAsOf(fundDetail.getAssetAllocationAsOf())
                .assetAllocationStatus(fundDetail.getAssetAllocationStatus())
                .oneMonthReturn(fundDetail.getOneMonthReturn())
                .threeMonthsReturn(fundDetail.getThreeMonthsReturn())
                .sixMonthsReturn(fundDetail.getSixMonthsReturn())
                .oneYearReturn(fundDetail.getOneYearReturn())
                .threeYearsReturn(fundDetail.getThreeYearsReturn())
                .catalogFetchedAt(fundDetail.getCatalogFetchedAt())
                .purchaseStatusFetchedAt(fundDetail.getPurchaseStatusFetchedAt())
                .periodReturnFetchedAt(fundDetail.getPeriodReturnFetchedAt())
                .topHoldingFetchedAt(fundDetail.getTopHoldingFetchedAt())
                .assetAllocationFetchedAt(fundDetail.getAssetAllocationFetchedAt())
                .topHoldingRefreshStatus(fundDetail.getTopHoldingRefreshStatus())
                .topHoldings(toTopHoldings(fundDetail.getTopHoldings()))
                .assetAllocations(toAssetAllocations(fundDetail.getAssetAllocations()))
                .build();
    }

    private List<PortfolioFundDetailDTO.TopHolding> toTopHoldings(List<PortfolioFundDetailResult.TopHolding> topHoldings) {
        if (topHoldings == null) {
            return null;
        }
        return topHoldings.stream()
                .map(topHolding -> topHolding == null ? null : PortfolioFundDetailDTO.TopHolding.builder()
                        .rankNo(topHolding.getRankNo())
                        .stockName(topHolding.getStockName())
                        .stockCode(topHolding.getStockCode())
                        .market(topHolding.getMarket())
                        .changePercent(topHolding.getChangePercent())
                        .refreshedAt(topHolding.getRefreshedAt())
                        .quoteStatus(topHolding.getQuoteStatus())
                        .holdingRatio(topHolding.getHoldingRatio())
                        .quarterChangeType(topHolding.getQuarterChangeType())
                        .quarterChangeValue(topHolding.getQuarterChangeValue())
                        .build())
                .toList();
    }

    private List<PortfolioFundDetailDTO.AssetAllocation> toAssetAllocations(
            List<PortfolioFundDetailResult.AssetAllocation> assetAllocations) {
        if (assetAllocations == null) {
            return null;
        }
        return assetAllocations.stream()
                .map(allocation -> allocation == null ? null : PortfolioFundDetailDTO.AssetAllocation.builder()
                        .assetType(allocation.getAssetType())
                        .assetTypeName(allocation.getAssetTypeName())
                        .allocationRatio(allocation.getAllocationRatio())
                        .displayOrder(allocation.getDisplayOrder())
                        .build())
                .toList();
    }

}
