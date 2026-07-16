package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundTopHoldingDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundTopHoldingPO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FundDataRepositoryTest {

    @Test
    public void upsertCatalogsMapsDomainFundsAndUsesOneDaoBatch() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        setField(repository, "fundDao", fundDao);

        repository.upsertCatalogs(List.of(
                FundCurrentDataAggregate.FundDetail.builder()
                        .fundCode("000001")
                        .fundName("测试基金一")
                        .fundType("混合型")
                        .pinyinAbbr("CSJJY")
                        .pinyinFull("CESHIJIJINYI")
                        .catalogFetchedAt(LocalDateTime.of(2026, 7, 16, 10, 0))
                        .build(),
                FundCurrentDataAggregate.FundDetail.builder()
                        .fundCode("000002")
                        .fundName("测试基金二")
                        .build()));

        Assert.assertEquals(2, fundDao.catalogBatch.size());
        Assert.assertEquals("000001", fundDao.catalogBatch.get(0).getFundCode());
        Assert.assertEquals("测试基金一", fundDao.catalogBatch.get(0).getFundName());
        Assert.assertEquals("混合型", fundDao.catalogBatch.get(0).getFundType());
        Assert.assertEquals("CSJJY", fundDao.catalogBatch.get(0).getPinyinAbbr());
        Assert.assertEquals("CESHIJIJINYI", fundDao.catalogBatch.get(0).getPinyinFull());
        Assert.assertNotNull(fundDao.catalogBatch.get(0).getCatalogFetchedAt());
        Assert.assertEquals("000002", fundDao.catalogBatch.get(1).getFundCode());
    }

    @Test
    public void updateTopHoldingSnapshotUpdatesExistingRankInsertsNewRankAndDeletesStaleRank() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        FakeFundTopHoldingDao fundTopHoldingDao = new FakeFundTopHoldingDao();
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
        repository.updateTopHoldingSnapshot(FundCurrentDataAggregate.FundDetail.builder()
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
                .build(), false);

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
    }

    @Test
    public void updateTopHoldingSnapshotClearsExistingTopHoldingsWhenRequested() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        FakeFundTopHoldingDao fundTopHoldingDao = new FakeFundTopHoldingDao();
        setField(repository, "fundDao", fundDao);
        setField(repository, "fundTopHoldingDao", fundTopHoldingDao);
        repository.updateTopHoldingSnapshot(FundCurrentDataAggregate.FundDetail.builder()
                .fundCode("000001")
                .topHoldingsAsOf(java.sql.Date.valueOf("2026-06-30"))
                .publicHoldingsStatus("no_public_stock_holdings")
                .topHoldings(List.of())
                .build(), true);

        Assert.assertEquals("000001", fundTopHoldingDao.deletedFundCode);
    }

    @Test
    public void updateTopHoldingSnapshotUsesLastDuplicateRankOnlyOnce() throws Exception {
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
        repository.updateTopHoldingSnapshot(FundCurrentDataAggregate.FundDetail.builder()
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
                .build(), false);

        Assert.assertEquals(1, fundTopHoldingDao.updated.size());
        Assert.assertEquals(Long.valueOf(101L), fundTopHoldingDao.updated.get(0).getId());
        Assert.assertEquals("last", fundTopHoldingDao.updated.get(0).getStockCode());
        Assert.assertTrue(fundTopHoldingDao.inserted.isEmpty());
        Assert.assertTrue(fundTopHoldingDao.deletedIds.isEmpty());
        Assert.assertNull(fundTopHoldingDao.deletedFundCode);
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
        private List<FundPO> catalogBatch = List.of();
        private List<FundPO> fundItems = List.of();

        @Override public void upsertCatalog(FundPO fundPO) { upserted = fundPO; }
        @Override public void upsertCatalogBatch(List<FundPO> funds) { catalogBatch = funds; }
        @Override public int updatePurchaseStatus(FundPO fundPO) { upserted = fundPO; return 1; }
        @Override public int updatePeriodReturn(FundPO fundPO) { upserted = fundPO; return 1; }
        @Override public int updateTopHoldingMetadata(FundPO fundPO) { upserted = fundPO; return 1; }
        @Override public int updateLastDetailViewTime(Collection<String> fundCodes, java.util.Date viewedAt) { return fundCodes.size(); }
        @Override public List<String> selectTopHoldingRefreshTargets(java.util.Date viewedSince) { return List.of(); }

        @Override
        public FundPO selectById(Long id) {
            return null;
        }

        @Override
        public List<FundPO> selectByFundCodes(Collection<String> fundCodes) {
            return fundItems;
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

}
