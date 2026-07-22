package com.echoamoy.holdlens.server.cases.marketasset.impl;

import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetQueryResult;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MarketAssetQueryCaseImplTest {

    @Test
    public void aggregatesUserWatchlistAndPreservesZeroWithoutWrites() throws Exception {
        FakePortfolio portfolio = new FakePortfolio();
        MarketAssetQueryCaseImpl service = new MarketAssetQueryCaseImpl();
        set(service, "portfolioRepository", portfolio);
        set(service, "fundDataRepository", new FakeFund());
        set(service, "stockMarketRepository", new FakeStock());

        MarketAssetQueryResult.Watchlist result = service.queryWatchlist(1001L, null);
        MarketAssetQueryResult.Watchlist fundsOnly = service.queryWatchlist(1001L, "fund");
        MarketAssetQueryResult.StockDetail stock = service.queryStockDetail(1001L, "stock:US_STOCK:DEMO");

        Assert.assertEquals(Integer.valueOf(1), result.getFundCount());
        Assert.assertEquals(Integer.valueOf(1), result.getStockCount());
        Assert.assertEquals("fund:000001", result.getItems().get(0).getAssetRef());
        Assert.assertEquals(0, result.getItems().get(0).getChangePercent().compareTo(BigDecimal.ZERO));
        Assert.assertEquals("stock:US_STOCK:DEMO", result.getItems().get(1).getAssetRef());
        Assert.assertEquals(1, fundsOnly.getItems().size());
        Assert.assertEquals(Integer.valueOf(1), fundsOnly.getFundCount());
        Assert.assertEquals(Integer.valueOf(1), fundsOnly.getStockCount());
        Assert.assertEquals("行情可能延迟", stock.getDelayNotice());
        Assert.assertNull(stock.getPeRatio());
        Assert.assertEquals(0, portfolio.writeCount);
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakePortfolio implements IPortfolioRepository {
        private int writeCount;
        @Override public List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId) { return List.of(); }
        @Override public void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets) { writeCount++; }
        @Override public WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind) {
            return WatchlistAssetEntity.builder().userId(userId).assetCode(assetCode).assetKind(assetKind).build();
        }
        @Override public List<WatchlistAssetEntity> queryWatchlistAssets(Long userId, String kind) {
            List<WatchlistAssetEntity> values = List.of(
                    WatchlistAssetEntity.builder().id(2L).userId(userId).assetKind("fund").assetCode("000001").build(),
                    WatchlistAssetEntity.builder().id(1L).userId(userId).assetKind("stock").assetCode("DEMO").market("US_STOCK").build());
            return kind == null ? values : values.stream().filter(value -> kind.equals(value.getAssetKind())).toList();
        }
    }

    private static class FakeFund implements IFundDataRepository {
        @Override public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) {
            return Map.of("000001", FundCurrentDataAggregate.FundDetail.builder().fundCode("000001")
                    .fundName("示例基金").fundType("混合型").unitNav(BigDecimal.ONE)
                    .dailyGrowthRate(BigDecimal.ZERO).build());
        }
        @Override public Set<String> queryExistingFundCodes(Collection<String> fundCodes) { return Set.copyOf(fundCodes); }
        @Override public void upsertCatalogs(List<FundCurrentDataAggregate.FundDetail> funds) { }
    }

    private static class FakeStock implements IStockMarketRepository {
        private StockMarketEntity value() {
            return StockMarketEntity.builder().stockCode("DEMO").market("US_STOCK").stockName("示例公司")
                    .currency("USD").volumeUnit("SHARE").latestPrice(BigDecimal.TEN).changePercent(BigDecimal.ZERO)
                    .volume(0L).refreshedAt(LocalDateTime.of(2026, 7, 17, 22, 41)).build();
        }
        @Override public void registerQuoteTargets(List<StockMarketEntity> quoteTargets) { }
        @Override public void upsertMarkets(List<StockMarketEntity> markets) { }
        @Override public Map<String, StockMarketEntity> queryByStockKeys(Collection<String> stockKeys) { return Map.of("DEMO#US_STOCK", value()); }
        @Override public Set<String> queryExistingStockKeys(Collection<String> stockKeys) { return Set.copyOf(stockKeys); }
        @Override public StockMarketEntity queryOne(String stockCode, String market) { return value(); }
    }
}
