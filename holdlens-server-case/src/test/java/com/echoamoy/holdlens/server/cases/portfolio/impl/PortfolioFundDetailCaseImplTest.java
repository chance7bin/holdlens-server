package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.funddata.model.entity.FundRefreshTargetEntity;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteEntity;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteTargetEntity;
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
        Assert.assertEquals("available", result.getHoldings().get(0).getFundDetail().getDetailStatus());
        Assert.assertEquals("missing", result.getHoldings().get(1).getFundDetail().getDetailStatus());
	        Assert.assertEquals("available", result.getHoldings().get(0).getFundDetail().getTopHoldings().get(0).getQuoteStatus());
	        Assert.assertEquals(new BigDecimal("0.50"), result.getHoldings().get(0).getFundDetail().getTopHoldings().get(0).getDailyReturn());
	        Assert.assertEquals("available", result.getHoldings().get(0).getFundDetail().getTopHoldings().get(1).getQuoteStatus());
	        Assert.assertEquals(new BigDecimal("0.10"), result.getHoldings().get(0).getFundDetail().getTopHoldings().get(1).getDailyReturn());
	        Assert.assertEquals(new BigDecimal("123.45"), result.getHoldings().get(0).getAmount());
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
    }

    private static class FakeFundDataRepository implements IFundDataRepository {
        @Override
        public void saveCurrentData(FundCurrentDataAggregate aggregate) {
        }

        @Override
        public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) {
            Assert.assertTrue(fundCodes.contains("000001"));
            Assert.assertTrue(fundCodes.contains("161725"));
            return Map.of(
                    "000001", FundCurrentDataAggregate.FundDetail.builder()
                            .fundCode("000001")
                            .fundName("测试基金")
                            .updateTime(LocalDateTime.now())
	                            .topHoldings(List.of(
	                                    FundCurrentDataAggregate.TopHolding.builder()
	                                            .rankNo(1)
	                                            .stockCode("600000")
	                                            .market("1")
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
        public List<FundRefreshTargetEntity> queryRefreshTargetsAfterId(Long lastId, int limit) {
            return List.of();
        }
    }

    private static class FakeStockMarketRepository implements IStockMarketRepository {
        @Override
        public List<StockQuoteTargetEntity> queryAllQuoteTargets() {
            return List.of();
        }

        @Override
        public List<StockQuoteTargetEntity> queryRefreshTargetsAfterId(Long lastId, int limit) {
            return List.of();
        }

        @Override
        public void registerQuoteTargets(List<StockQuoteEntity> quoteTargets) {
        }

        @Override
        public void upsertQuotes(List<StockQuoteEntity> quotes) {
        }

        @Override
	        public Map<String, StockQuoteEntity> queryByStockKeys(java.util.Collection<String> stockKeys) {
	            Assert.assertTrue(stockKeys.contains("600000#1"));
	            Assert.assertTrue(stockKeys.contains("000001#"));
	            return Map.of(
	                    "600000#1", StockQuoteEntity.builder()
	                            .stockCode("600000")
	                            .market("1")
	                            .dailyReturn(new BigDecimal("0.50"))
	                            .build(),
	                    "000001#", StockQuoteEntity.builder()
	                            .stockCode("000001")
	                            .market(null)
	                            .dailyReturn(new BigDecimal("0.10"))
	                            .build());
	        }
    }

}
