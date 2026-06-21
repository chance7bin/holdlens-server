package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IStockMarketCurrentDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockMarketCurrentPO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
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
                .market("1")
                .stockName("测试股份")
                .tradeDate(new Date())
                .dailyReturn(new BigDecimal("0.50"))
                .quoteTime(new Date())
                .build()));

        Assert.assertEquals(1, stockMarketCurrentDao.targetUpserts.size());
        Assert.assertEquals(0, stockMarketCurrentDao.quoteUpserts.size());
        StockMarketCurrentPO target = stockMarketCurrentDao.targetUpserts.get(0);
        Assert.assertEquals("600000", target.getStockCode());
        Assert.assertEquals("1", target.getMarket());
        Assert.assertEquals("测试股份", target.getStockName());
        Assert.assertNull(target.getTradeDate());
        Assert.assertNull(target.getDailyReturn());
        Assert.assertNull(target.getQuoteTime());
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeStockMarketCurrentDao implements IStockMarketCurrentDao {
        private final List<StockMarketCurrentPO> quoteUpserts = new ArrayList<>();
        private final List<StockMarketCurrentPO> targetUpserts = new ArrayList<>();

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
            return List.of();
        }

        @Override
        public List<StockMarketCurrentPO> selectByStockKeys(Collection<String> stockKeys) {
            return List.of();
        }
    }
}
