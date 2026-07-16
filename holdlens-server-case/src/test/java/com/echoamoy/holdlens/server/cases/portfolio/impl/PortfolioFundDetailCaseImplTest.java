package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PortfolioFundDetailCaseImplTest {

    @Test
    public void queryPortfolioFundDetailsOnlyReturnsHeldFundDetails() throws Exception {
        PortfolioFundDetailCaseImpl fundDetailCase = new PortfolioFundDetailCaseImpl();
        setField(fundDetailCase, "portfolioRepository", new FakePortfolioRepository());
        setField(fundDetailCase, "fundDataRepository", new FakeFundDataRepository());
        setField(fundDetailCase, "stockMarketRepository", new FakeStockMarketRepository());

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
        Assert.assertEquals(Set.of("000001"), fundRepository.viewedCodes);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
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
        @Override
        public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) {
            Assert.assertTrue(fundCodes.contains("000001"));
            return Map.of(
                    "000001", FundCurrentDataAggregate.FundDetail.builder()
                            .fundCode("000001")
                            .fundName("测试基金")
                            .updateTime(LocalDateTime.now())
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
