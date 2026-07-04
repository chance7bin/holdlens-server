package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IStockMarketDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockMarketPO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StockMarketRepositoryTest {

    @Test
    public void registerQuoteTargetsUsesTargetUpsertWithoutMarketFields() throws Exception {
        StockMarketRepository repository = new StockMarketRepository();
        FakeStockMarketDao stockMarketDao = new FakeStockMarketDao();
        setField(repository, "stockMarketDao", stockMarketDao);

        repository.registerQuoteTargets(List.of(StockMarketEntity.builder()
                .stockCode("600000")
                .market(StockMarketEntity.MARKET_A_SHARE)
                .stockName("测试股份")
                .latestPrice(new BigDecimal("10.23"))
                .build()));

        Assert.assertEquals(1, stockMarketDao.targetUpserts.size());
        Assert.assertEquals(0, stockMarketDao.marketUpserts.size());
        StockMarketPO target = stockMarketDao.targetUpserts.get(0);
        Assert.assertEquals("600000", target.getStockCode());
        Assert.assertEquals(StockMarketEntity.MARKET_A_SHARE, target.getMarket());
        Assert.assertEquals("测试股份", target.getStockName());
        Assert.assertEquals(StockMarketEntity.STATUS_ACTIVE, target.getStatus());
        Assert.assertNull(target.getLatestPrice());
    }

    @Test
    public void upsertMarketsMapsAllCurrentMarketFields() throws Exception {
        StockMarketRepository repository = new StockMarketRepository();
        FakeStockMarketDao stockMarketDao = new FakeStockMarketDao();
        setField(repository, "stockMarketDao", stockMarketDao);

        repository.upsertMarkets(List.of(StockMarketEntity.builder()
                .stockCode("600000")
                .market(StockMarketEntity.MARKET_A_SHARE)
                .exchangeCode("SH")
                .providerMarketCode("1")
                .stockName("测试股份")
                .latestPrice(new BigDecimal("10.23"))
                .changePercent(new BigDecimal("1.25"))
                .volume(1234567L)
                .peRatio(new BigDecimal("56.7890"))
                .listingDate(LocalDate.of(1999, 1, 22))
                .status(StockMarketEntity.STATUS_ACTIVE)
                .refreshedAt(LocalDateTime.of(2026, 6, 18, 10, 4, 30))
                .build()));

        Assert.assertEquals(1, stockMarketDao.marketUpserts.size());
        StockMarketPO market = stockMarketDao.marketUpserts.get(0);
        Assert.assertEquals("SH", market.getExchangeCode());
        Assert.assertEquals("1", market.getProviderMarketCode());
        Assert.assertEquals(new BigDecimal("10.23"), market.getLatestPrice());
        Assert.assertEquals(new BigDecimal("1.25"), market.getChangePercent());
        Assert.assertEquals(Long.valueOf(1234567L), market.getVolume());
        Assert.assertEquals(new BigDecimal("56.7890"), market.getPeRatio());
        Assert.assertEquals(LocalDate.of(1999, 1, 22), market.getListingDate());
        Assert.assertEquals(LocalDateTime.of(2026, 6, 18, 10, 4, 30), market.getRefreshedAt());
    }

    @Test
    public void queryByStockKeysUsesStockCodeAndBusinessMarketKey() throws Exception {
        StockMarketRepository repository = new StockMarketRepository();
        FakeStockMarketDao stockMarketDao = new FakeStockMarketDao();
        stockMarketDao.stockMarkets = List.of(StockMarketPO.builder()
                .stockCode("600000")
                .market(StockMarketEntity.MARKET_US_STOCK)
                .changePercent(new BigDecimal("0.10"))
                .peRatio(new BigDecimal("56.7890"))
                .listingDate(LocalDate.of(1999, 1, 22))
                .build());
        setField(repository, "stockMarketDao", stockMarketDao);

        Assert.assertTrue(repository.queryByStockKeys(List.of("600000#US_STOCK")).containsKey("600000#US_STOCK"));
        Assert.assertEquals(new BigDecimal("0.10"),
                repository.queryByStockKeys(List.of("600000#US_STOCK")).get("600000#US_STOCK").getChangePercent());
        Assert.assertEquals(LocalDate.of(1999, 1, 22),
                repository.queryByStockKeys(List.of("600000#US_STOCK")).get("600000#US_STOCK").getListingDate());
    }

    @Test
    public void queryExistingStockKeysReturnsBusinessMarketKeys() throws Exception {
        StockMarketRepository repository = new StockMarketRepository();
        FakeStockMarketDao stockMarketDao = new FakeStockMarketDao();
        stockMarketDao.stockMarkets = List.of(StockMarketPO.builder()
                .stockCode("600000")
                .market(StockMarketEntity.MARKET_A_SHARE)
                .build());
        setField(repository, "stockMarketDao", stockMarketDao);

        Assert.assertTrue(repository.queryExistingStockKeys(List.of("600000#A_SHARE")).contains("600000#A_SHARE"));
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeStockMarketDao implements IStockMarketDao {
        private final List<StockMarketPO> marketUpserts = new ArrayList<>();
        private final List<StockMarketPO> targetUpserts = new ArrayList<>();
        private List<StockMarketPO> stockMarkets = List.of();

        @Override
        public void upsert(StockMarketPO stockMarketPO) {
            marketUpserts.add(stockMarketPO);
        }

        @Override
        public void upsertTarget(StockMarketPO stockMarketPO) {
            targetUpserts.add(stockMarketPO);
        }

        @Override
        public List<StockMarketPO> selectByStockKeys(Collection<String> stockKeys) {
            return stockMarkets;
        }
    }
}
