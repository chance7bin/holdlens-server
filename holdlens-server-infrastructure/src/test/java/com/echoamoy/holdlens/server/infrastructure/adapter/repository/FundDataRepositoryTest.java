package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundDetailItemDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundTopHoldingDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IProcessingLogDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundDetailItemPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundTopHoldingPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingLogPO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public class FundDataRepositoryTest {

    @Test
    public void saveCurrentDataUpsertsFundAndReplacesTopHoldingsAndLogsWarnings() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDetailItemDao fundDetailItemDao = new FakeFundDetailItemDao();
        FakeFundTopHoldingDao fundTopHoldingDao = new FakeFundTopHoldingDao();
        FakeProcessingLogDao processingLogDao = new FakeProcessingLogDao();
        setField(repository, "fundDetailItemDao", fundDetailItemDao);
        setField(repository, "fundTopHoldingDao", fundTopHoldingDao);
        setField(repository, "processingLogDao", processingLogDao);

        LocalDateTime generatedAt = LocalDateTime.of(2026, 6, 16, 18, 0);

        repository.saveCurrentData(FundCurrentDataAggregate.builder()
                .schemaVersion("fund-detail-refresh-result/v2")
                .generatedAt(generatedAt)
                .status("partial_failed")
                .sourceRefId("task_1")
                .funds(List.of(FundCurrentDataAggregate.FundDetail.builder()
                        .fundCode("000001")
                        .fundName("测试基金")
                        .topHoldings(List.of(FundCurrentDataAggregate.TopHolding.builder()
                                .rankNo(1)
                                .stockCode("600000")
                                .market("1")
                                .build()))
                        .build()))
                .warnings(List.of(FundCurrentDataAggregate.RefreshWarning.builder()
                        .module("fund_refresh")
                        .event("provider_fund_failed")
                        .message("provider failed for one fund")
                        .severity("error")
                        .build()))
                .build());

        Assert.assertEquals("000001", fundDetailItemDao.upserted.getFundCode());
        Assert.assertEquals("000001", fundTopHoldingDao.deletedFundCode);
        Assert.assertEquals("000001", fundTopHoldingDao.inserted.getFundCode());
        Assert.assertEquals("600000", fundTopHoldingDao.inserted.getStockCode());
        Assert.assertEquals("task_1", processingLogDao.inserted.getSourceRefId());
        Assert.assertEquals("fund_refresh", processingLogDao.inserted.getModule());
        Assert.assertEquals("provider_fund_failed", processingLogDao.inserted.getEvent());
        Assert.assertEquals("provider failed for one fund", processingLogDao.inserted.getMessage());
        Assert.assertEquals("error", processingLogDao.inserted.getSeverity());
    }

    @Test
    public void queryRefreshTargetsAfterIdMapsFundTargets() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDetailItemDao fundDetailItemDao = new FakeFundDetailItemDao();
        fundDetailItemDao.refreshTargets = List.of(
                FundDetailItemPO.builder().id(10L).fundCode("000001").build(),
                FundDetailItemPO.builder().id(11L).fundCode("161725").build());
        setField(repository, "fundDetailItemDao", fundDetailItemDao);

        Assert.assertEquals(2, repository.queryRefreshTargetsAfterId(9L, 20).size());
        Assert.assertEquals("000001", repository.queryRefreshTargetsAfterId(9L, 20).get(0).getFundCode());
        Assert.assertEquals(Long.valueOf(9L), fundDetailItemDao.lastId);
        Assert.assertEquals(20, fundDetailItemDao.limit);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeFundDetailItemDao implements IFundDetailItemDao {
        private FundDetailItemPO upserted;
        private Long lastId;
        private int limit;
        private List<FundDetailItemPO> refreshTargets = List.of();

        @Override
        public void upsert(FundDetailItemPO fundDetailItemPO) {
            upserted = fundDetailItemPO;
        }

        @Override
        public FundDetailItemPO selectById(Long id) {
            return null;
        }

        @Override
        public List<FundDetailItemPO> selectByFundCodes(Collection<String> fundCodes) {
            return List.of();
        }

        @Override
        public List<FundDetailItemPO> selectRefreshTargetsAfterId(Long lastId, int limit) {
            this.lastId = lastId;
            this.limit = limit;
            return refreshTargets;
        }
    }

    private static class FakeFundTopHoldingDao implements IFundTopHoldingDao {
        private String deletedFundCode;
        private FundTopHoldingPO inserted;

        @Override
        public void insert(FundTopHoldingPO fundTopHoldingPO) {
            inserted = fundTopHoldingPO;
        }

        @Override
        public void deleteByFundCode(String fundCode) {
            deletedFundCode = fundCode;
        }

        @Override
        public FundTopHoldingPO selectById(Long id) {
            return null;
        }

        @Override
        public List<FundTopHoldingPO> selectByFundCodes(Collection<String> fundCodes) {
            return List.of();
        }

        @Override
        public List<FundTopHoldingPO> selectByStockCode(String stockCode) {
            return List.of();
        }
    }

    private static class FakeProcessingLogDao implements IProcessingLogDao {
        private ProcessingLogPO inserted;

        @Override
        public void insert(ProcessingLogPO processingLogPO) {
            inserted = processingLogPO;
        }

        @Override
        public ProcessingLogPO selectById(Long id) {
            return null;
        }

        @Override
        public List<ProcessingLogPO> selectBySourceRefId(String sourceRefId) {
            return List.of();
        }
    }

}
