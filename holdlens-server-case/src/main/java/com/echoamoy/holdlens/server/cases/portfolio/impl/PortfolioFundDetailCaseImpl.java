package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.IPortfolioFundDetailCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteEntity;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PortfolioFundDetailCaseImpl implements IPortfolioFundDetailCase {

    private static final int STALE_DAYS = 7;
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private IPortfolioRepository portfolioRepository;

    @Resource
    private IFundDataRepository fundDataRepository;

    @Resource
    private IStockMarketRepository stockMarketRepository;

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
        Map<String, FundCurrentDataAggregate.FundDetail> currentDetails = fundDataRepository.queryCurrentDetails(fundCodes);
        Map<String, StockQuoteEntity> stockQuotes = stockMarketRepository.queryByStockKeys(collectStockKeys(currentDetails));
        return PortfolioFundDetailResult.builder()
                .userId(userId)
                .holdings(holdings.stream()
                        .map(holding -> toHoldingDetail(holding, currentDetails.get(holding.fundCodeOrNull()), stockQuotes))
                        .toList())
                .build();
    }

    private PortfolioFundDetailResult.HoldingDetail toHoldingDetail(PortfolioHoldingEntity holding,
                                                                    FundCurrentDataAggregate.FundDetail fundDetail,
                                                                    Map<String, StockQuoteEntity> stockQuotes) {
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
                .fundDetail(toFundDetail(holding.fundCodeOrNull(), fundDetail, stockQuotes))
                .build();
    }

    private PortfolioFundDetailResult.FundDetail toFundDetail(String fundCode,
                                                             FundCurrentDataAggregate.FundDetail detail,
                                                             Map<String, StockQuoteEntity> stockQuotes) {
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
                .returnsAsOf(detail.getReturnsAsOf())
                .topHoldingsAsOf(detail.getTopHoldingsAsOf())
                .publicHoldingsStatus(detail.getPublicHoldingsStatus())
                .oneMonthReturn(detail.getOneMonthReturn())
                .threeMonthsReturn(detail.getThreeMonthsReturn())
                .sixMonthsReturn(detail.getSixMonthsReturn())
                .oneYearReturn(detail.getOneYearReturn())
                .threeYearsReturn(detail.getThreeYearsReturn())
                .topHoldings(detail.getTopHoldings() == null ? List.of() : detail.getTopHoldings().stream()
                        .map(topHolding -> toTopHolding(topHolding,
                                stockQuotes.get(stockKey(topHolding.getStockCode(), normalizeNullable(topHolding.getMarket())))))
                        .toList())
                .build();
    }

    private PortfolioFundDetailResult.TopHolding toTopHolding(FundCurrentDataAggregate.TopHolding topHolding,
                                                              StockQuoteEntity stockQuote) {
        return PortfolioFundDetailResult.TopHolding.builder()
                .rankNo(topHolding.getRankNo())
                .stockName(topHolding.getStockName())
                .stockCode(topHolding.getStockCode())
                .market(topHolding.getMarket())
                .dailyReturn(stockQuote == null ? null : stockQuote.getDailyReturn())
                .quoteTradeDate(stockQuote == null ? null : stockQuote.getTradeDate())
                .quoteTime(stockQuote == null ? null : toDate(stockQuote.getQuoteTime()))
                .quoteStatus(stockQuote == null ? "missing" : "available")
                .holdingRatio(topHolding.getHoldingRatio())
                .quarterChangeType(topHolding.getQuarterChangeType())
                .quarterChangeValue(topHolding.getQuarterChangeValue())
                .build();
    }

    private Set<String> collectStockKeys(Map<String, FundCurrentDataAggregate.FundDetail> details) {
        Set<String> stockKeys = new LinkedHashSet<>();
        for (FundCurrentDataAggregate.FundDetail detail : details.values()) {
            if (detail.getTopHoldings() == null) {
                continue;
            }
            for (FundCurrentDataAggregate.TopHolding topHolding : detail.getTopHoldings()) {
                if (!isBlank(topHolding.getStockCode())) {
                    stockKeys.add(stockKey(topHolding.getStockCode().trim(), normalizeNullable(topHolding.getMarket())));
                }
            }
        }
        return stockKeys;
    }

    private String stockKey(String stockCode, String market) {
        return stockCode + "#" + (market == null ? "" : market);
    }

    private String normalizeNullable(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isStale(FundCurrentDataAggregate.FundDetail detail) {
        return detail.getUpdateTime() != null
                && detail.getUpdateTime().isBefore(LocalDateTime.now(BEIJING_ZONE).minus(STALE_DAYS, ChronoUnit.DAYS));
    }

    private Date toDate(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atZone(BEIJING_ZONE).toInstant());
    }

}
