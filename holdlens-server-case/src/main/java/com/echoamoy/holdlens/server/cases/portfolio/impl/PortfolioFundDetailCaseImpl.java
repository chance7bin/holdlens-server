package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.IPortfolioFundDetailCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import com.echoamoy.holdlens.server.types.common.DateTimeUtils;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionException;

import static com.echoamoy.holdlens.server.types.common.StringUtils.isBlank;
import static com.echoamoy.holdlens.server.types.common.StringUtils.normalizeNullable;

@Slf4j
@Service
public class PortfolioFundDetailCaseImpl implements IPortfolioFundDetailCase {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private IPortfolioRepository portfolioRepository;

    @Resource
    private IFundDataRepository fundDataRepository;

    @Resource
    private IStockMarketRepository stockMarketRepository;

    @Resource
    private IFundSliceRefreshCase fundSliceRefreshCase;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Value("${holdlens.agent.fund-top-holding-refresh.detail-stale-days}")
    private int topHoldingStaleDays;

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
        Map<String, StockMarketEntity> stockMarkets = stockMarketRepository.queryByStockKeys(collectStockKeys(currentDetails));
        LocalDateTime viewedAt = LocalDateTime.now(BEIJING_ZONE);
        fundDataRepository.markDetailViewed(fundCodes, viewedAt);
        List<String> staleCodes = fundCodes.stream()
                .filter(code -> needsTopHoldingRefresh(currentDetails.get(code), viewedAt))
                .toList();
        List<String> allocationStaleCodes = fundCodes.stream()
                .filter(code -> currentDetails.get(code) != null)
                .filter(code -> needsAssetAllocationRefresh(currentDetails.get(code), viewedAt))
                .toList();
        PortfolioFundDetailResult result = PortfolioFundDetailResult.builder()
                .userId(userId)
                .holdings(holdings.stream()
                        .map(holding -> toHoldingDetail(holding, currentDetails.get(holding.fundCodeOrNull()), stockMarkets,
                                staleCodes.contains(holding.fundCodeOrNull())))
                        .toList())
                .build();
        dispatchTopHoldingRefreshBestEffort(staleCodes);
        dispatchAssetAllocationRefreshBestEffort(allocationStaleCodes);
        return result;
    }

    @Override
    public PortfolioFundDetailResult.FundDetail queryFundDetail(String fundCode) {
        if (isBlank(fundCode)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "基金代码不能为空");
        }
        String normalizedCode = fundCode.trim();
        FundCurrentDataAggregate.FundDetail detail = fundDataRepository
                .queryCurrentDetails(Set.of(normalizedCode)).get(normalizedCode);
        if (detail == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "基金目录中不存在该基金");
        }

        LocalDateTime viewedAt = LocalDateTime.now(BEIJING_ZONE);
        fundDataRepository.markDetailViewed(Set.of(normalizedCode), viewedAt);
        boolean stale = needsTopHoldingRefresh(detail, viewedAt);
        boolean allocationStale = needsAssetAllocationRefresh(detail, viewedAt);
        Map<String, StockMarketEntity> stockMarkets = stockMarketRepository
                .queryByStockKeys(collectStockKeys(Map.of(normalizedCode, detail)));
        PortfolioFundDetailResult.FundDetail result = toFundDetail(normalizedCode, detail, stockMarkets, stale);
        if (stale) {
            dispatchTopHoldingRefreshBestEffort(List.of(normalizedCode));
        }
        if (allocationStale) {
            dispatchAssetAllocationRefreshBestEffort(List.of(normalizedCode));
        }
        return result;
    }

    private PortfolioFundDetailResult.HoldingDetail toHoldingDetail(PortfolioHoldingEntity holding,
                                                                    FundCurrentDataAggregate.FundDetail fundDetail,
                                                                    Map<String, StockMarketEntity> stockMarkets,
                                                                    boolean refreshing) {
        return PortfolioFundDetailResult.HoldingDetail.builder()
                .recordId(holding.getRecordId())
                .assetRef(holding.getAssetRef())
                .assetCode(holding.getAssetCode())
                .assetName(holding.getAssetName())
                .assetKind(holding.getAssetKind())
                .assetType(holding.getAssetType())
                .amount(holding.getAmount())
                .currency(holding.getCurrency())
                .status(holding.getStatus())
                .fundDetail(toFundDetail(holding.fundCodeOrNull(), fundDetail, stockMarkets, refreshing))
                .build();
    }

    private PortfolioFundDetailResult.FundDetail toFundDetail(String fundCode,
                                                             FundCurrentDataAggregate.FundDetail detail,
                                                             Map<String, StockMarketEntity> stockMarkets,
                                                             boolean refreshing) {
        if (fundCode == null) {
            return PortfolioFundDetailResult.FundDetail.builder().detailStatus("unavailable")
                    .assetAllocationStatus("missing").assetAllocations(List.of()).build();
        }
        if (detail == null) {
            return PortfolioFundDetailResult.FundDetail.builder()
                    .fundCode(fundCode)
                    .detailStatus("missing")
                    .assetAllocationStatus("missing")
                    .assetAllocations(List.of())
                    .topHoldingRefreshStatus(refreshing ? "refreshing" : "missing")
                    .build();
        }
        return PortfolioFundDetailResult.FundDetail.builder()
                .fundCode(detail.getFundCode())
                .fundName(detail.getFundName())
                .fundType(detail.getFundType())
                .detailStatus(needsTopHoldingRefresh(detail, LocalDateTime.now(BEIJING_ZONE)) ? "stale" : "available")
                .buyStatus(detail.getBuyStatus())
                .dailyPurchaseLimit(detail.getDailyPurchaseLimit())
                .returnsAsOf(detail.getReturnsAsOf())
                .unitNav(detail.getUnitNav())
                .accumulatedNav(detail.getAccumulatedNav())
                .dailyGrowthRate(detail.getDailyGrowthRate())
                .returnCoverageStatus(detail.getReturnCoverageStatus())
                .topHoldingsAsOf(detail.getTopHoldingsAsOf())
                .publicHoldingsStatus(detail.getPublicHoldingsStatus())
                .assetAllocationAsOf(detail.getAssetAllocationAsOf())
                .assetAllocationStatus(detail.getAssetAllocationStatus() == null
                        ? "missing" : detail.getAssetAllocationStatus())
                .oneMonthReturn(detail.getOneMonthReturn())
                .threeMonthsReturn(detail.getThreeMonthsReturn())
                .sixMonthsReturn(detail.getSixMonthsReturn())
                .oneYearReturn(detail.getOneYearReturn())
                .threeYearsReturn(detail.getThreeYearsReturn())
                .catalogFetchedAt(DateTimeUtils.toBusinessDate(detail.getCatalogFetchedAt()))
                .purchaseStatusFetchedAt(DateTimeUtils.toBusinessDate(detail.getPurchaseStatusFetchedAt()))
                .periodReturnFetchedAt(DateTimeUtils.toBusinessDate(detail.getPeriodReturnFetchedAt()))
                .topHoldingFetchedAt(DateTimeUtils.toBusinessDate(detail.getTopHoldingFetchedAt()))
                .assetAllocationFetchedAt(DateTimeUtils.toBusinessDate(detail.getAssetAllocationFetchedAt()))
                .topHoldingRefreshStatus(refreshing ? "refreshing" : "current")
                .topHoldings(detail.getTopHoldings() == null ? List.of() : detail.getTopHoldings().stream()
                        .map(topHolding -> toTopHolding(topHolding,
                                stockMarkets.get(stockKey(topHolding.getStockCode(), normalizeNullable(topHolding.getMarket())))))
                        .toList())
                .assetAllocations(detail.getAssetAllocations() == null ? List.of() : detail.getAssetAllocations().stream()
                        .map(allocation -> PortfolioFundDetailResult.AssetAllocation.builder()
                                .assetType(allocation.getAssetType())
                                .assetTypeName(allocation.getAssetTypeName())
                                .allocationRatio(allocation.getAllocationRatio())
                                .displayOrder(allocation.getDisplayOrder())
                                .build())
                        .toList())
                .build();
    }

    private PortfolioFundDetailResult.TopHolding toTopHolding(FundCurrentDataAggregate.TopHolding topHolding,
                                                              StockMarketEntity stockMarket) {
        return PortfolioFundDetailResult.TopHolding.builder()
                .rankNo(topHolding.getRankNo())
                .stockName(topHolding.getStockName())
                .stockCode(topHolding.getStockCode())
                .market(topHolding.getMarket())
                .changePercent(stockMarket == null ? null : stockMarket.getChangePercent())
                .refreshedAt(stockMarket == null ? null : DateTimeUtils.toBusinessDate(stockMarket.getRefreshedAt()))
                .quoteStatus(stockMarket == null ? "missing" : "available")
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

    private boolean needsTopHoldingRefresh(FundCurrentDataAggregate.FundDetail detail, LocalDateTime now) {
        return detail == null || detail.getTopHoldingFetchedAt() == null
                || detail.getTopHoldingFetchedAt().isBefore(now.minus(Math.max(topHoldingStaleDays, 1), ChronoUnit.DAYS));
    }

    private boolean needsAssetAllocationRefresh(FundCurrentDataAggregate.FundDetail detail, LocalDateTime now) {
        if (detail == null || detail.getAssetAllocationStatus() == null
                || "missing".equals(detail.getAssetAllocationStatus())) {
            return true;
        }
        if ("unavailable".equals(detail.getAssetAllocationStatus())) {
            return detail.getAssetAllocationFetchedAt() == null
                    || !detail.getAssetAllocationFetchedAt().isAfter(now.minusDays(7));
        }
        if (!"available".equals(detail.getAssetAllocationStatus())) {
            return true;
        }
        LocalDate asOf = detail.getAssetAllocationAsOf() == null ? null
                : LocalDate.ofInstant(Instant.ofEpochMilli(detail.getAssetAllocationAsOf().getTime()), BEIJING_ZONE);
        return asOf == null || asOf.isBefore(latestEndedQuarter(now.toLocalDate()));
    }

    private LocalDate latestEndedQuarter(LocalDate date) {
        int firstMonthOfQuarter = ((date.getMonthValue() - 1) / 3) * 3 + 1;
        return LocalDate.of(date.getYear(), firstMonthOfQuarter, 1).minusDays(1);
    }

    private void dispatchTopHoldingRefreshBestEffort(List<String> staleCodes) {
        if (staleCodes.isEmpty() || threadPoolExecutor == null || fundSliceRefreshCase == null) return;
        try {
            threadPoolExecutor.execute(() -> {
                try {
                    fundSliceRefreshCase.dispatchTopHoldings(new ArrayList<>(staleCodes), "detail_view");
                } catch (RuntimeException exception) {
                    log.warn("基金详情异步重仓刷新派发失败 fundCodeCount={}", staleCodes.size());
                }
            });
        } catch (RejectedExecutionException exception) {
            log.warn("基金详情异步重仓刷新进入线程池失败 fundCodeCount={}", staleCodes.size());
        }
    }

    private void dispatchAssetAllocationRefreshBestEffort(List<String> staleCodes) {
        if (staleCodes.isEmpty() || threadPoolExecutor == null || fundSliceRefreshCase == null) return;
        try {
            threadPoolExecutor.execute(() -> {
                try {
                    fundSliceRefreshCase.dispatchAssetAllocations(new ArrayList<>(staleCodes), "detail_view");
                } catch (RuntimeException exception) {
                    log.warn("基金详情异步资产配置刷新派发失败 fundCodeCount={}", staleCodes.size());
                }
            });
        } catch (RejectedExecutionException exception) {
            log.warn("基金详情异步资产配置刷新进入线程池失败 fundCodeCount={}", staleCodes.size());
        }
    }

}
