package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.FundSliceRefreshCallbackCommand;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PortfolioFundDetailCaseImplTest {

    @Test
    public void queryPortfolioFundDetailsOnlyReturnsHeldFundDetails() throws Exception {
        PortfolioFundDetailCaseImpl fundDetailCase = new PortfolioFundDetailCaseImpl();
        FakeFundDataRepository fundRepository = new FakeFundDataRepository();
        fundRepository.assetAllocationAsOf = java.sql.Date.valueOf(latestEndedQuarter().minusMonths(3));
        FakeSliceCase sliceCase = new FakeSliceCase();
        setField(fundDetailCase, "portfolioRepository", new FakePortfolioRepository());
        setField(fundDetailCase, "fundDataRepository", fundRepository);
        setField(fundDetailCase, "stockMarketRepository", new FakeStockMarketRepository());
        setField(fundDetailCase, "fundSliceRefreshCase", sliceCase);
        setField(fundDetailCase, "threadPoolExecutor", new DirectExecutor());

        PortfolioFundDetailResult result = fundDetailCase.queryPortfolioFundDetails(1001L);

        Assert.assertEquals(1001L, result.getUserId().longValue());
        Assert.assertEquals(2, result.getHoldings().size());
        Assert.assertEquals("stale", result.getHoldings().get(0).getFundDetail().getDetailStatus());
        Assert.assertEquals("missing", result.getHoldings().get(1).getFundDetail().getDetailStatus());
	        Assert.assertEquals("available", result.getHoldings().get(0).getFundDetail().getTopHoldings().get(0).getQuoteStatus());
        Assert.assertEquals(new BigDecimal("0.50"), result.getHoldings().get(0).getFundDetail().getTopHoldings().get(0).getChangePercent());
        Assert.assertEquals("available", result.getHoldings().get(0).getFundDetail().getTopHoldings().get(1).getQuoteStatus());
        Assert.assertEquals(new BigDecimal("0.10"), result.getHoldings().get(0).getFundDetail().getTopHoldings().get(1).getChangePercent());
        Assert.assertEquals(new BigDecimal("123.45"), result.getHoldings().get(0).getAmount());
        Assert.assertEquals(List.of("000001"), sliceCase.assetAllocationCodes);
    }

    @Test
    public void queryCatalogFundDetailReturnsDatabaseSnapshotAndMarksRecentView() throws Exception {
        PortfolioFundDetailCaseImpl fundDetailCase = new PortfolioFundDetailCaseImpl();
        FakeFundDataRepository fundRepository = new FakeFundDataRepository();
        setField(fundDetailCase, "portfolioRepository", new FakePortfolioRepository());
        setField(fundDetailCase, "fundDataRepository", fundRepository);
        setField(fundDetailCase, "stockMarketRepository", new FakeStockMarketRepository());

        PortfolioFundDetailResult.FundDetail result = fundDetailCase.queryFundDetail(" 000001 ");

        Assert.assertEquals("000001", result.getFundCode());
        Assert.assertEquals("stale", result.getDetailStatus());
        Assert.assertEquals("refreshing", result.getTopHoldingRefreshStatus());
        Assert.assertEquals("available", result.getAssetAllocationStatus());
        Assert.assertEquals(2, result.getAssetAllocations().size());
        Assert.assertEquals("unknown", result.getAssetAllocations().get(0).getAssetType());
        Assert.assertEquals("其他资产A", result.getAssetAllocations().get(0).getAssetTypeName());
        Assert.assertEquals(Set.of("000001"), fundRepository.viewedCodes);
    }

    @Test
    public void detailViewDispatchesAssetAllocationWhenQuarterIsStaleWithoutBlockingResponse() throws Exception {
        PortfolioFundDetailCaseImpl fundDetailCase = new PortfolioFundDetailCaseImpl();
        FakeFundDataRepository fundRepository = new FakeFundDataRepository();
        fundRepository.assetAllocationAsOf = java.sql.Date.valueOf(latestEndedQuarter().minusMonths(3));
        FakeSliceCase sliceCase = new FakeSliceCase();
        setField(fundDetailCase, "portfolioRepository", new FakePortfolioRepository());
        setField(fundDetailCase, "fundDataRepository", fundRepository);
        setField(fundDetailCase, "stockMarketRepository", new FakeStockMarketRepository());
        setField(fundDetailCase, "fundSliceRefreshCase", sliceCase);
        setField(fundDetailCase, "threadPoolExecutor", new DirectExecutor());

        PortfolioFundDetailResult.FundDetail result = fundDetailCase.queryFundDetail("000001");

        Assert.assertEquals("000001", result.getFundCode());
        Assert.assertEquals(List.of("000001"), sliceCase.assetAllocationCodes);
    }

    @Test
    public void detailViewRespectsUnavailableSevenDayBackoff() throws Exception {
        PortfolioFundDetailCaseImpl fundDetailCase = new PortfolioFundDetailCaseImpl();
        FakeFundDataRepository fundRepository = new FakeFundDataRepository();
        fundRepository.assetAllocationStatus = "unavailable";
        fundRepository.assetAllocationAsOf = null;
        fundRepository.assetAllocationFetchedAt = LocalDateTime.now();
        FakeSliceCase sliceCase = new FakeSliceCase();
        setField(fundDetailCase, "portfolioRepository", new FakePortfolioRepository());
        setField(fundDetailCase, "fundDataRepository", fundRepository);
        setField(fundDetailCase, "stockMarketRepository", new FakeStockMarketRepository());
        setField(fundDetailCase, "fundSliceRefreshCase", sliceCase);
        setField(fundDetailCase, "threadPoolExecutor", new DirectExecutor());

        fundDetailCase.queryFundDetail("000001");

        Assert.assertTrue(sliceCase.assetAllocationCodes.isEmpty());
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static LocalDate latestEndedQuarter() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        int firstMonthOfQuarter = ((today.getMonthValue() - 1) / 3) * 3 + 1;
        return LocalDate.of(today.getYear(), firstMonthOfQuarter, 1).minusDays(1);
    }

    private static class FakePortfolioRepository implements IPortfolioRepository {
        @Override
        public List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId) {
            return List.of(
                    PortfolioHoldingEntity.builder()
                            .userId(userId)
                            .holdingId(1L)
                            .assetCode("000001")
                            .assetName("测试基金")
                            .amount(new BigDecimal("123.45"))
                            .status("active")
                            .build(),
                    PortfolioHoldingEntity.builder()
                            .userId(userId)
                            .holdingId(2L)
                            .assetCode("161725")
                            .assetName("缺失基金")
                            .status("active")
                            .build());
        }

        @Override
        public void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets) {
        }

        @Override
        public WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind) {
            return null;
        }
    }

    private static class FakeFundDataRepository implements IFundDataRepository {
        private Set<String> viewedCodes = Set.of();
        private java.util.Date assetAllocationAsOf = java.sql.Date.valueOf(latestEndedQuarter());
        private String assetAllocationStatus = "available";
        private LocalDateTime assetAllocationFetchedAt = LocalDateTime.of(2026, 7, 16, 10, 0);
        @Override
        public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) {
            Assert.assertTrue(fundCodes.contains("000001"));
            return Map.of(
                    "000001", FundCurrentDataAggregate.FundDetail.builder()
                            .fundCode("000001")
                            .fundName("测试基金")
                            .updateTime(LocalDateTime.now())
	                            .assetAllocationAsOf(assetAllocationAsOf)
	                            .assetAllocationStatus(assetAllocationStatus)
	                            .assetAllocationFetchedAt(assetAllocationFetchedAt)
	                            .assetAllocations(List.of(
	                                    FundCurrentDataAggregate.AssetAllocation.builder()
	                                            .assetType("unknown").assetTypeName("其他资产A")
	                                            .allocationRatio(new BigDecimal("1.10")).displayOrder(1).build(),
	                                    FundCurrentDataAggregate.AssetAllocation.builder()
	                                            .assetType("unknown").assetTypeName("其他资产B")
	                                            .allocationRatio(new BigDecimal("2.20")).displayOrder(2).build()))
	                            .topHoldings(List.of(
	                                    FundCurrentDataAggregate.TopHolding.builder()
	                                            .rankNo(1)
	                                            .stockCode("600000")
                                            .market(StockMarketEntity.MARKET_A_SHARE)
	                                            .build(),
	                                    FundCurrentDataAggregate.TopHolding.builder()
	                                            .rankNo(2)
	                                            .stockCode("000001")
	                                            .market(null)
	                                            .build()))
                            .build(),
                    "999999", FundCurrentDataAggregate.FundDetail.builder()
                            .fundCode("999999")
                            .fundName("未持有基金")
                            .updateTime(LocalDateTime.now())
	                            .build());
        }

        @Override
        public Set<String> queryExistingFundCodes(java.util.Collection<String> fundCodes) {
            return Set.of();
        }

        @Override
        public void upsertCatalogs(List<FundCurrentDataAggregate.FundDetail> funds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markDetailViewed(java.util.Collection<String> fundCodes, LocalDateTime viewedAt) {
            viewedCodes = Set.copyOf(fundCodes);
        }
    }

    private static class FakeSliceCase implements IFundSliceRefreshCase {
        private List<String> assetAllocationCodes = new ArrayList<>();
        public FundRefreshTaskResult scheduleCatalog(String trigger) { return null; }
        public FundRefreshTaskResult schedulePurchaseStatus(String trigger) { return null; }
        public FundRefreshTaskResult schedulePeriodReturn(String trigger) { return null; }
        public List<FundRefreshTaskResult> scheduleTopHoldings(String trigger, int batchSize) { return List.of(); }
        public List<FundRefreshTaskResult> scheduleAssetAllocations(String trigger, int batchSize) { return List.of(); }
        public FundRefreshTaskResult dispatchTopHoldings(List<String> fundCodes, String trigger) { return null; }
        public FundRefreshTaskResult dispatchAssetAllocations(List<String> fundCodes, String trigger) {
            assetAllocationCodes = List.copyOf(fundCodes);
            return null;
        }
        public FundRefreshTaskResult handleCallback(String taskType, FundSliceRefreshCallbackCommand command) { return null; }
        public int closeTimedOutCallbacks(int timeoutMinutes) { return 0; }
    }

    private static class DirectExecutor extends ThreadPoolExecutor {
        DirectExecutor() {
            super(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        }
        @Override public void execute(Runnable command) { command.run(); }
    }

    private static class FakeStockMarketRepository implements IStockMarketRepository {
        @Override
        public void registerQuoteTargets(List<StockMarketEntity> quoteTargets) {
        }

        @Override
        public void upsertMarkets(List<StockMarketEntity> markets) {
        }

        @Override
        public Map<String, StockMarketEntity> queryByStockKeys(java.util.Collection<String> stockKeys) {
            Assert.assertTrue(stockKeys.contains("600000#A_SHARE"));
            Assert.assertTrue(stockKeys.contains("000001#"));
            return Map.of(
                    "600000#A_SHARE", StockMarketEntity.builder()
                            .stockCode("600000")
                            .market(StockMarketEntity.MARKET_A_SHARE)
                            .changePercent(new BigDecimal("0.50"))
                            .build(),
                    "000001#", StockMarketEntity.builder()
                            .stockCode("000001")
                            .market(null)
                            .changePercent(new BigDecimal("0.10"))
                            .build());
        }

        @Override
        public Set<String> queryExistingStockKeys(java.util.Collection<String> stockKeys) {
            return Set.of();
        }
    }

}
