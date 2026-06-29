package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IStockMarketCurrentDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockMarketCurrentPO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class StockMarketRepositoryTest {

    @Test
	    public void registerQuoteTargetsUsesTargetUpsertWithoutQuoteFields() throws Exception {
	        StockMarketRepository repository = new StockMarketRepository();
	        FakeStockMarketCurrentDao stockMarketCurrentDao = new FakeStockMarketCurrentDao();
	        setField(repository, "stockMarketCurrentDao", stockMarketCurrentDao);

	        repository.registerQuoteTargets(List.of(StockQuoteEntity.builder()
	                .stockCode("600000")
                .market(null)
                .stockName(null)
                .tradeDate(new Date())
                .dailyReturn(new BigDecimal("0.50"))
                .quoteTime(LocalDateTime.of(2026, 6, 18, 10, 4, 30))
                .build()));

        Assert.assertEquals(1, stockMarketCurrentDao.targetUpserts.size());
        Assert.assertEquals(0, stockMarketCurrentDao.quoteUpserts.size());
	        StockMarketCurrentPO target = stockMarketCurrentDao.targetUpserts.get(0);
	        Assert.assertEquals("600000", target.getStockCode());
	        Assert.assertNull(target.getMarket());
	        Assert.assertNull(target.getStockName());
	        Assert.assertNull(target.getTradeDate());
	        Assert.assertNull(target.getDailyReturn());
	        Assert.assertNull(target.getQuoteTime());
	    }

	    @Test
	    public void queryAllQuoteTargetsIncludesNullMarketTargets() throws Exception {
	        StockMarketRepository repository = new StockMarketRepository();
	        FakeStockMarketCurrentDao stockMarketCurrentDao = new FakeStockMarketCurrentDao();
	        stockMarketCurrentDao.targets = List.of(StockMarketCurrentPO.builder()
	                .stockCode("000001")
	                .market(null)
	                .build());
	        setField(repository, "stockMarketCurrentDao", stockMarketCurrentDao);

	        Assert.assertEquals(1, repository.queryAllQuoteTargets().size());
	        Assert.assertEquals("000001", repository.queryAllQuoteTargets().get(0).getStockCode());
	        Assert.assertNull(repository.queryAllQuoteTargets().get(0).getMarket());
	    }

	    @Test
    public void queryByStockKeysUsesEmptyMarketKeyForNullMarket() throws Exception {
	        StockMarketRepository repository = new StockMarketRepository();
	        FakeStockMarketCurrentDao stockMarketCurrentDao = new FakeStockMarketCurrentDao();
	        stockMarketCurrentDao.stockQuotes = List.of(StockMarketCurrentPO.builder()
	                .stockCode("000001")
	                .market(null)
	                .dailyReturn(new BigDecimal("0.10"))
	                .build());
	        setField(repository, "stockMarketCurrentDao", stockMarketCurrentDao);

        Assert.assertTrue(repository.queryByStockKeys(List.of("000001#")).containsKey("000001#"));
    }

    @Test
    public void queryExistingStockKeysReturnsEmptyMarketKeyForNullMarket() throws Exception {
        StockMarketRepository repository = new StockMarketRepository();
        FakeStockMarketCurrentDao stockMarketCurrentDao = new FakeStockMarketCurrentDao();
        stockMarketCurrentDao.stockQuotes = List.of(StockMarketCurrentPO.builder()
                .stockCode("000001")
                .market(null)
                .build());
        setField(repository, "stockMarketCurrentDao", stockMarketCurrentDao);

        Assert.assertTrue(repository.queryExistingStockKeys(List.of("000001#")).contains("000001#"));
    }

    @Test
    public void queryRefreshTargetsAfterIdMapsStockTargets() throws Exception {
        StockMarketRepository repository = new StockMarketRepository();
        FakeStockMarketCurrentDao stockMarketCurrentDao = new FakeStockMarketCurrentDao();
        stockMarketCurrentDao.refreshTargets = List.of(
                StockMarketCurrentPO.builder().id(7L).stockCode("600000").market("1").build(),
                StockMarketCurrentPO.builder().id(8L).stockCode("000001").market("0").build());
        setField(repository, "stockMarketCurrentDao", stockMarketCurrentDao);

        Assert.assertEquals(2, repository.queryRefreshTargetsAfterId(6L, 50).size());
        Assert.assertEquals(Long.valueOf(7L), repository.queryRefreshTargetsAfterId(6L, 50).get(0).getId());
        Assert.assertEquals("600000", repository.queryRefreshTargetsAfterId(6L, 50).get(0).getStockCode());
        Assert.assertEquals(Long.valueOf(6L), stockMarketCurrentDao.lastId);
        Assert.assertEquals(50, stockMarketCurrentDao.limit);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeStockMarketCurrentDao implements IStockMarketCurrentDao {
	        private final List<StockMarketCurrentPO> quoteUpserts = new ArrayList<>();
	        private final List<StockMarketCurrentPO> targetUpserts = new ArrayList<>();
	        private List<StockMarketCurrentPO> targets = List.of();
	        private List<StockMarketCurrentPO> stockQuotes = List.of();
            private List<StockMarketCurrentPO> refreshTargets = List.of();
            private Long lastId;
            private int limit;

        @Override
        public void upsert(StockMarketCurrentPO stockMarketCurrentPO) {
            quoteUpserts.add(stockMarketCurrentPO);
        }

        @Override
        public void upsertTarget(StockMarketCurrentPO stockMarketCurrentPO) {
            targetUpserts.add(stockMarketCurrentPO);
        }

        @Override
	        public List<StockMarketCurrentPO> selectAllTargets() {
		            return targets;
		        }

            @Override
            public List<StockMarketCurrentPO> selectRefreshTargetsAfterId(Long lastId, int limit) {
                this.lastId = lastId;
                this.limit = limit;
                return refreshTargets;
            }

		        @Override
		        public List<StockMarketCurrentPO> selectByStockKeys(Collection<String> stockKeys) {
	            return stockQuotes;
	        }
    }
}
