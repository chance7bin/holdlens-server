package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundDetailSnapshotAggregate;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PortfolioFundDetailCaseImplTest {

    @Test
    public void queryPortfolioFundDetailsOnlyReturnsHeldFundDetails() throws Exception {
        PortfolioFundDetailCaseImpl fundDetailCase = new PortfolioFundDetailCaseImpl();
        setField(fundDetailCase, "portfolioRepository", new FakePortfolioRepository());
        setField(fundDetailCase, "fundDataRepository", new FakeFundDataRepository());

        PortfolioFundDetailResult result = fundDetailCase.queryPortfolioFundDetails(1001L);

        Assert.assertEquals(1001L, result.getUserId().longValue());
        Assert.assertEquals(2, result.getHoldings().size());
        Assert.assertEquals("available", result.getHoldings().get(0).getFundDetail().getDetailStatus());
        Assert.assertEquals("missing", result.getHoldings().get(1).getFundDetail().getDetailStatus());
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
        public Long saveSnapshot(FundDetailSnapshotAggregate aggregate) {
            return 1L;
        }

        @Override
        public Map<String, FundDetailSnapshotAggregate.FundDetail> queryLatestDetails(Set<String> fundCodes) {
            Assert.assertTrue(fundCodes.contains("000001"));
            Assert.assertTrue(fundCodes.contains("161725"));
            return Map.of(
                    "000001", FundDetailSnapshotAggregate.FundDetail.builder()
                            .fundCode("000001")
                            .fundName("测试基金")
                            .generatedAt(new Date())
                            .topHoldings(List.of())
                            .build(),
                    "999999", FundDetailSnapshotAggregate.FundDetail.builder()
                            .fundCode("999999")
                            .fundName("未持有基金")
                            .generatedAt(new Date())
                            .build());
        }
    }

}
