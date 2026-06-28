package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IPortfolioFundDetailService;
import com.echoamoy.holdlens.server.api.dto.PortfolioFundDetailDTO;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.portfolio.IPortfolioFundDetailCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import org.springframework.web.bind.annotation.GetMapping;
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
                .detailStatus(fundDetail.getDetailStatus())
                .buyStatus(fundDetail.getBuyStatus())
                .dailyPurchaseLimit(fundDetail.getDailyPurchaseLimit())
                .returnsAsOf(fundDetail.getReturnsAsOf())
                .topHoldingsAsOf(fundDetail.getTopHoldingsAsOf())
                .publicHoldingsStatus(fundDetail.getPublicHoldingsStatus())
                .oneMonthReturn(fundDetail.getOneMonthReturn())
                .threeMonthsReturn(fundDetail.getThreeMonthsReturn())
                .sixMonthsReturn(fundDetail.getSixMonthsReturn())
                .oneYearReturn(fundDetail.getOneYearReturn())
                .threeYearsReturn(fundDetail.getThreeYearsReturn())
                .topHoldings(toTopHoldings(fundDetail.getTopHoldings()))
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
                        .dailyReturn(topHolding.getDailyReturn())
                        .quoteTradeDate(topHolding.getQuoteTradeDate())
                        .quoteTime(topHolding.getQuoteTime())
                        .quoteStatus(topHolding.getQuoteStatus())
                        .holdingRatio(topHolding.getHoldingRatio())
                        .quarterChangeType(topHolding.getQuarterChangeType())
                        .quarterChangeValue(topHolding.getQuarterChangeValue())
                        .build())
                .toList();
    }

}
