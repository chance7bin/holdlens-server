package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundAssetAllocationDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundTopHoldingDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundAssetAllocationPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundTopHoldingPO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

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

    @Test
    public void replaceAssetAllocationSnapshotUpdatesMetadataAndReplacesRowsWithoutTouchingTopHoldings() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        FakeFundTopHoldingDao topHoldingDao = new FakeFundTopHoldingDao();
        FakeFundAssetAllocationDao allocationDao = new FakeFundAssetAllocationDao();
        fundDao.lockedAssetAllocation = FundPO.builder().fundCode("000001").build();
        setField(repository, "fundDao", fundDao);
        setField(repository, "fundTopHoldingDao", topHoldingDao);
        setField(repository, "fundAssetAllocationDao", allocationDao);

        boolean replaced = repository.replaceAssetAllocationSnapshot(FundCurrentDataAggregate.FundDetail.builder()
                .fundCode("000001")
                .assetAllocationAsOf(java.sql.Date.valueOf("2026-06-30"))
                .assetAllocationStatus("available")
                .assetAllocationFetchedAt(LocalDateTime.of(2026, 7, 16, 10, 0))
                .assetAllocations(List.of(
                        FundCurrentDataAggregate.AssetAllocation.builder()
                                .assetType("unknown").assetTypeName("其他资产A")
                                .allocationRatio(new BigDecimal("1.1000")).displayOrder(1).build(),
                        FundCurrentDataAggregate.AssetAllocation.builder()
                                .assetType("unknown").assetTypeName("其他资产B")
                                .allocationRatio(new BigDecimal("2.2000")).displayOrder(2).build()))
                .build());

        Assert.assertTrue(replaced);
        Assert.assertEquals("available", fundDao.assetAllocationMetadata.getAssetAllocationStatus());
        Assert.assertEquals("000001", allocationDao.deletedFundCode);
        Assert.assertEquals(2, allocationDao.inserted.size());
        Assert.assertEquals("unknown", allocationDao.inserted.get(0).getAssetType());
        Assert.assertEquals("其他资产A", allocationDao.inserted.get(0).getAssetTypeName());
        Assert.assertEquals("其他资产B", allocationDao.inserted.get(1).getAssetTypeName());
        Assert.assertNull(topHoldingDao.deletedFundCode);
        Assert.assertTrue(topHoldingDao.inserted.isEmpty());
    }

    @Test
    public void queryCurrentDetailsLoadsAllocationsInOneBatchAndMapsStatus() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        FakeFundTopHoldingDao topHoldingDao = new FakeFundTopHoldingDao();
        FakeFundAssetAllocationDao allocationDao = new FakeFundAssetAllocationDao();
        fundDao.fundItems = List.of(FundPO.builder().fundCode("000001")
                .assetAllocationStatus("available")
                .assetAllocationAsOf(java.sql.Date.valueOf("2026-06-30")).build());
        allocationDao.existing = List.of(FundAssetAllocationPO.builder().fundCode("000001")
                .assetType("stock").assetTypeName("股票")
                .allocationRatio(new BigDecimal("70.0000")).displayOrder(1).build());
        setField(repository, "fundDao", fundDao);
        setField(repository, "fundTopHoldingDao", topHoldingDao);
        setField(repository, "fundAssetAllocationDao", allocationDao);

        FundCurrentDataAggregate.FundDetail detail = repository.queryCurrentDetails(
                java.util.Set.of("000001")).get("000001");

        Assert.assertEquals("available", detail.getAssetAllocationStatus());
        Assert.assertEquals(1, detail.getAssetAllocations().size());
        Assert.assertEquals(1, allocationDao.selectCount);
    }

    @Test
    public void assetAllocationTargetQueryPassesFreshnessBoundaries() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        fundDao.assetAllocationTargets = List.of("000001", "000002");
        setField(repository, "fundDao", fundDao);

        List<String> result = repository.queryAssetAllocationRefreshTargets(
                LocalDateTime.of(2026, 4, 17, 10, 0), LocalDate.of(2026, 6, 30),
                LocalDateTime.of(2026, 7, 9, 10, 0));

        Assert.assertEquals(List.of("000001", "000002"), result);
        Assert.assertEquals(java.sql.Date.valueOf("2026-06-30"), fundDao.latestEndedQuarter);
        Assert.assertNotNull(fundDao.unavailableRetryBefore);
    }

    @Test
    public void assetAllocationReplacementDeclaresTransactionBoundary() throws Exception {
        Transactional transactional = FundDataRepository.class
                .getMethod("replaceAssetAllocationSnapshot", FundCurrentDataAggregate.FundDetail.class)
                .getAnnotation(Transactional.class);
        Assert.assertNotNull(transactional);
        Assert.assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }

    @Test
    public void olderConcurrentAssetAllocationCannotDeleteNewerSnapshot() throws Exception {
        FundDataRepository repository = new FundDataRepository();
        FakeFundDao fundDao = new FakeFundDao();
        FakeFundAssetAllocationDao allocationDao = new FakeFundAssetAllocationDao();
        fundDao.lockedAssetAllocation = FundPO.builder().fundCode("000001")
                .assetAllocationAsOf(java.sql.Date.valueOf("2026-09-30")).build();
        setField(repository, "fundDao", fundDao);
        setField(repository, "fundAssetAllocationDao", allocationDao);

        boolean replaced = repository.replaceAssetAllocationSnapshot(FundCurrentDataAggregate.FundDetail.builder()
                .fundCode("000001").assetAllocationAsOf(java.sql.Date.valueOf("2026-06-30"))
                .assetAllocationStatus("available")
                .assetAllocations(List.of(FundCurrentDataAggregate.AssetAllocation.builder()
                        .assetType("stock").assetTypeName("股票")
                        .allocationRatio(BigDecimal.TEN).displayOrder(1).build()))
                .build());

        Assert.assertFalse(replaced);
        Assert.assertNull(fundDao.assetAllocationMetadata);
        Assert.assertNull(allocationDao.deletedFundCode);
        Assert.assertTrue(allocationDao.inserted.isEmpty());
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
        private FundPO assetAllocationMetadata;
        private List<String> assetAllocationTargets = List.of();
        private java.util.Date latestEndedQuarter;
        private java.util.Date unavailableRetryBefore;
        private FundPO lockedAssetAllocation;

        @Override public void upsertCatalog(FundPO fundPO) { upserted = fundPO; }
        @Override public void upsertCatalogBatch(List<FundPO> funds) { catalogBatch = funds; }
        @Override public int updatePurchaseStatus(FundPO fundPO) { upserted = fundPO; return 1; }
        @Override public int updatePeriodReturn(FundPO fundPO) { upserted = fundPO; return 1; }
        @Override public int updateTopHoldingMetadata(FundPO fundPO) { upserted = fundPO; return 1; }
        @Override public int updateAssetAllocationMetadata(FundPO fundPO) { assetAllocationMetadata = fundPO; return 1; }
        @Override public int markAssetAllocationUnavailable(String fundCode, java.util.Date fetchedAt) { return 1; }
        @Override public int updateLastDetailViewTime(Collection<String> fundCodes, java.util.Date viewedAt) { return fundCodes.size(); }
        @Override public List<String> selectTopHoldingRefreshTargets(java.util.Date viewedSince) { return List.of(); }
        @Override public List<String> selectAssetAllocationRefreshTargets(
                java.util.Date viewedSince, java.util.Date latestEndedQuarter,
                java.util.Date unavailableRetryBefore) {
            this.latestEndedQuarter = latestEndedQuarter;
            this.unavailableRetryBefore = unavailableRetryBefore;
            return assetAllocationTargets;
        }

        @Override
        public FundPO selectById(Long id) {
            return null;
        }

        @Override
        public FundPO selectAssetAllocationMetadataForUpdate(String fundCode) {
            return lockedAssetAllocation;
        }

        @Override
        public List<FundPO> selectByFundCodes(Collection<String> fundCodes) {
            return fundItems;
        }

        @Override
        public List<FundPO> search(String keyword, int limit) {
            return fundItems;
        }

    }

    private static class FakeFundAssetAllocationDao implements IFundAssetAllocationDao {
        private String deletedFundCode;
        private List<FundAssetAllocationPO> inserted = new ArrayList<>();
        private List<FundAssetAllocationPO> existing = List.of();
        private int selectCount;

        @Override
        public void insertBatch(List<FundAssetAllocationPO> allocations) {
            inserted.addAll(allocations);
        }

        @Override
        public int deleteByFundCode(String fundCode) {
            deletedFundCode = fundCode;
            return 1;
        }

        @Override
        public List<FundAssetAllocationPO> selectByFundCodes(Collection<String> fundCodes) {
            selectCount++;
            return existing;
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
