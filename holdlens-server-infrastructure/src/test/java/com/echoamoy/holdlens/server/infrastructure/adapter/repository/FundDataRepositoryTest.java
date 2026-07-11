package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.funddata.model.entity.FundRefreshTargetEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundTopHoldingDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IProcessingLogDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundTopHoldingPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.ProcessingLogPO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FundDataRepositoryTest {

    @Test
    public void saveCurrentDataUpdatesExistingRankInsertsNewRankAndDeletesStaleRank() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        FakeFundTopHoldingDao fundTopHoldingDao = new FakeFundTopHoldingDao();
        FakeProcessingLogDao processingLogDao = new FakeProcessingLogDao();
        fundTopHoldingDao.existing = List.of(
                FundTopHoldingPO.builder()
                        .id(101L)
                        .fundCode("000001")
                        .rankNo(1)
                        .stockCode("600001")
                        .build(),
                FundTopHoldingPO.builder()
                        .id(102L)
                        .fundCode("000001")
                        .rankNo(2)
                        .stockCode("600002")
                        .build());
        setField(repository, "fundDao", fundDao);
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
                        .topHoldings(List.of(
                                FundCurrentDataAggregate.TopHolding.builder()
                                        .rankNo(1)
                                        .stockCode("600000")
                                        .market("1")
                                        .build(),
                                FundCurrentDataAggregate.TopHolding.builder()
                                        .rankNo(3)
                                        .stockCode("600003")
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

        Assert.assertEquals("000001", fundDao.upserted.getFundCode());
        Assert.assertEquals(1, fundTopHoldingDao.updated.size());
        Assert.assertEquals(Long.valueOf(101L), fundTopHoldingDao.updated.get(0).getId());
        Assert.assertEquals(Integer.valueOf(1), fundTopHoldingDao.updated.get(0).getRankNo());
        Assert.assertEquals("600000", fundTopHoldingDao.updated.get(0).getStockCode());
        Assert.assertEquals(1, fundTopHoldingDao.inserted.size());
        Assert.assertEquals(Integer.valueOf(3), fundTopHoldingDao.inserted.get(0).getRankNo());
        Assert.assertEquals("600003", fundTopHoldingDao.inserted.get(0).getStockCode());
        Assert.assertEquals(List.of(102L), fundTopHoldingDao.deletedIds);
        Assert.assertNull(fundTopHoldingDao.deletedFundCode);
        Assert.assertEquals("task_1", processingLogDao.inserted.getSourceRefId());
        Assert.assertEquals("fund_refresh", processingLogDao.inserted.getModule());
        Assert.assertEquals("provider_fund_failed", processingLogDao.inserted.getEvent());
        Assert.assertEquals("provider failed for one fund", processingLogDao.inserted.getMessage());
        Assert.assertEquals("error", processingLogDao.inserted.getSeverity());
    }

    @Test
    public void saveCurrentDataDeletesAllExistingTopHoldingsWhenIncomingListIsEmpty() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        FakeFundTopHoldingDao fundTopHoldingDao = new FakeFundTopHoldingDao();
        fundTopHoldingDao.existing = List.of(FundTopHoldingPO.builder()
                .id(101L)
                .fundCode("000001")
                .rankNo(1)
                .build());
        setField(repository, "fundDao", fundDao);
        setField(repository, "fundTopHoldingDao", fundTopHoldingDao);
        setField(repository, "processingLogDao", new FakeProcessingLogDao());

        repository.saveCurrentData(FundCurrentDataAggregate.builder()
                .funds(List.of(FundCurrentDataAggregate.FundDetail.builder()
                        .fundCode("000001")
                        .topHoldings(List.of())
                        .build()))
                .build());

        Assert.assertEquals("000001", fundTopHoldingDao.deletedFundCode);
        Assert.assertTrue(fundTopHoldingDao.updated.isEmpty());
        Assert.assertTrue(fundTopHoldingDao.inserted.isEmpty());
        Assert.assertTrue(fundTopHoldingDao.deletedIds.isEmpty());
    }

    @Test
    public void saveCurrentDataUsesLastDuplicateRankOnlyOnce() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        FakeFundTopHoldingDao fundTopHoldingDao = new FakeFundTopHoldingDao();
        fundTopHoldingDao.existing = List.of(FundTopHoldingPO.builder()
                .id(101L)
                .fundCode("000001")
                .rankNo(1)
                .stockCode("old")
                .build());
        setField(repository, "fundDao", fundDao);
        setField(repository, "fundTopHoldingDao", fundTopHoldingDao);
        setField(repository, "processingLogDao", new FakeProcessingLogDao());

        repository.saveCurrentData(FundCurrentDataAggregate.builder()
                .funds(List.of(FundCurrentDataAggregate.FundDetail.builder()
                        .fundCode("000001")
                        .topHoldings(List.of(
                                FundCurrentDataAggregate.TopHolding.builder()
                                        .rankNo(1)
                                        .stockCode("first")
                                        .build(),
                                FundCurrentDataAggregate.TopHolding.builder()
                                        .rankNo(1)
                                        .stockCode("last")
                                        .build()))
                        .build()))
                .build());

        Assert.assertEquals(1, fundTopHoldingDao.updated.size());
        Assert.assertEquals(Long.valueOf(101L), fundTopHoldingDao.updated.get(0).getId());
        Assert.assertEquals("last", fundTopHoldingDao.updated.get(0).getStockCode());
        Assert.assertTrue(fundTopHoldingDao.inserted.isEmpty());
        Assert.assertTrue(fundTopHoldingDao.deletedIds.isEmpty());
        Assert.assertNull(fundTopHoldingDao.deletedFundCode);
    }

    @Test
    public void queryRefreshTargetsAfterIdMapsFundTargets() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        fundDao.refreshTargets = List.of(
                FundPO.builder().id(10L).fundCode("000001").build(),
                FundPO.builder().id(11L).fundCode("161725").build());
        setField(repository, "fundDao", fundDao);

        Assert.assertEquals(2, repository.queryRefreshTargetsAfterId(9L, 20).size());
        Assert.assertEquals("000001", repository.queryRefreshTargetsAfterId(9L, 20).get(0).getFundCode());
        Assert.assertEquals(Long.valueOf(9L), fundDao.lastId);
        Assert.assertEquals(20, fundDao.limit);
    }

    @Test
    public void registerRefreshTargetsUsesTargetUpsertWithoutCodeNameFallback() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        setField(repository, "fundDao", fundDao);

        repository.registerRefreshTargets(List.of(FundRefreshTargetEntity.builder()
                .fundCode("000001")
                .fundName(null)
                .build()));

        Assert.assertEquals("000001", fundDao.targetUpserted.getFundCode());
        Assert.assertNull(fundDao.targetUpserted.getFundName());
    }

    @Test
    public void queryExistingFundCodesReturnsOnlyExistingCodes() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        fundDao.fundItems = List.of(
                FundPO.builder().fundCode("000001").build(),
                FundPO.builder().fundCode("161725").build());
        setField(repository, "fundDao", fundDao);

        Assert.assertTrue(repository.queryExistingFundCodes(List.of("000001", "999999")).contains("000001"));
        Assert.assertFalse(repository.queryExistingFundCodes(List.of("000001", "999999")).contains("999999"));
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeFundDao implements IFundDao {
        private FundPO upserted;
        private FundPO targetUpserted;
        private Long lastId;
        private int limit;
        private List<FundPO> refreshTargets = List.of();
        private List<FundPO> fundItems = List.of();

        @Override
        public void upsert(FundPO fundPO) {
            upserted = fundPO;
        }

        @Override
        public void upsertTarget(FundPO fundPO) {
            targetUpserted = fundPO;
        }

        @Override
        public FundPO selectById(Long id) {
            return null;
        }

        @Override
        public List<FundPO> selectByFundCodes(Collection<String> fundCodes) {
            return fundItems;
        }

        @Override
        public List<FundPO> selectRefreshTargetsAfterId(Long lastId, int limit) {
            this.lastId = lastId;
            this.limit = limit;
            return refreshTargets;
        }
    }

    private static class FakeFundTopHoldingDao implements IFundTopHoldingDao {
        private String deletedFundCode;
        private List<Long> deletedIds = new ArrayList<>();
        private List<FundTopHoldingPO> existing = List.of();
        private List<FundTopHoldingPO> inserted = new ArrayList<>();
        private List<FundTopHoldingPO> updated = new ArrayList<>();

        @Override
        public void insert(FundTopHoldingPO fundTopHoldingPO) {
            inserted.add(fundTopHoldingPO);
        }

        @Override
        public void update(FundTopHoldingPO fundTopHoldingPO) {
            updated.add(fundTopHoldingPO);
        }

        @Override
        public void deleteByFundCode(String fundCode) {
            deletedFundCode = fundCode;
        }

        @Override
        public void deleteByIds(Collection<Long> ids) {
            deletedIds.addAll(ids);
        }

        @Override
        public FundTopHoldingPO selectById(Long id) {
            return null;
        }

        @Override
        public List<FundTopHoldingPO> selectByFundCodes(Collection<String> fundCodes) {
            return existing;
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
