package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.model.AssetManagementCommand;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordChangeEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssetManagementCaseImplTest {

    private AssetManagementCaseImpl assetCase;
    private FakePortfolioRepository portfolio;
    private FakeStockRepository stocks;

    @Before
    public void setUp() throws Exception {
        assetCase = new AssetManagementCaseImpl();
        portfolio = new FakePortfolioRepository();
        setField(assetCase, "portfolioRepository", portfolio);
        setField(assetCase, "fundDataRepository", new FakeFundRepository());
        stocks = new FakeStockRepository();
        setField(assetCase, "stockMarketRepository", stocks);
    }

    @Test
    public void createsConcreteFundWithNameSnapshotAndAppendOnlyHistory() {
        AssetRecordEntity record = assetCase.createRecord(AssetManagementCommand.CreateRecord.builder()
                .userId(1L).catalogId(5L).assetRef("fund:000001").recordName("客户端名称")
                .amount(new BigDecimal("1000")).currency("CNY").build());

        Assert.assertEquals(Long.valueOf(101L), record.getId());
        Assert.assertEquals("公开示例基金", record.getRecordName());
        Assert.assertEquals("fund:000001", record.getAssetRef());
        Assert.assertEquals(Long.valueOf(88L), record.getAssetId());
        Assert.assertEquals(1, portfolio.changes.size());
        Assert.assertEquals(AssetRecordChangeEntity.CREATE, portfolio.changes.get(0).getChangeType());
    }

    @Test
    public void partiallySplitsUnspecifiedFundInOneOperation() {
        AssetRecordEntity source = AssetRecordEntity.builder().id(10L).userId(1L).catalogId(5L)
                .catalogCode(AssetCatalogEntity.CODE_FUND).balanceDirection(AssetCatalogEntity.DIRECTION_ADD)
                .recordName("未细分基金").assetKind(AssetRecordEntity.KIND_FUND)
                .amount(new BigDecimal("100000")).currency("CNY").status(AssetRecordEntity.STATUS_ACTIVE).build();
        portfolio.record = source;

        AssetRecordEntity target = assetCase.splitRecord(AssetManagementCommand.SplitRecord.builder()
                .userId(1L).sourceRecordId(10L).assetRef("fund:000001")
                .amount(new BigDecimal("30000")).build());

        Assert.assertEquals(0, new BigDecimal("70000").compareTo(source.getAmount()));
        Assert.assertEquals(AssetRecordEntity.STATUS_ACTIVE, source.getStatus());
        Assert.assertEquals(0, new BigDecimal("30000").compareTo(target.getAmount()));
        Assert.assertEquals(2, portfolio.changes.size());
        Assert.assertEquals(portfolio.changes.get(0).getOperationId(), portfolio.changes.get(1).getOperationId());
        Assert.assertEquals(AssetRecordChangeEntity.SPLIT_OUT, portfolio.changes.get(0).getChangeType());
        Assert.assertEquals(AssetRecordChangeEntity.SPLIT_IN, portfolio.changes.get(1).getChangeType());
    }

    @Test(expected = IllegalStateException.class)
    public void investmentGroupCannotRecordAmount() {
        portfolio.catalog = AssetCatalogEntity.builder().id(4L).catalogCode(AssetCatalogEntity.CODE_INVESTMENT_ASSET)
                .catalogName("投资资产").catalogScope(AssetCatalogEntity.SCOPE_SYSTEM)
                .balanceDirection(AssetCatalogEntity.DIRECTION_ADD).status(AssetCatalogEntity.STATUS_ENABLED).build();
        portfolio.childCount = 2;

        assetCase.createRecord(AssetManagementCommand.CreateRecord.builder()
                .userId(1L).catalogId(4L).recordName("非法记录").amount(BigDecimal.ONE).currency("CNY").build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInactivePublicStock() {
        portfolio.catalog = AssetCatalogEntity.builder().id(6L).parentId(4L)
                .catalogCode(AssetCatalogEntity.CODE_STOCK).catalogName("股票")
                .catalogScope(AssetCatalogEntity.SCOPE_SYSTEM).balanceDirection(AssetCatalogEntity.DIRECTION_ADD)
                .status(AssetCatalogEntity.STATUS_ENABLED).build();
        stocks.stock = StockMarketEntity.builder().id(99L).stockCode("DEMO").market("US_STOCK")
                .stockName("停用股票").status(StockMarketEntity.STATUS_MISSING_FROM_REFRESH).build();

        assetCase.createRecord(AssetManagementCommand.CreateRecord.builder()
                .userId(1L).catalogId(6L).assetRef("stock:US_STOCK:DEMO")
                .amount(BigDecimal.ONE).currency("USD").build());
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakePortfolioRepository implements IPortfolioRepository {
        private AssetCatalogEntity catalog = AssetCatalogEntity.builder().id(5L).parentId(4L)
                .catalogCode(AssetCatalogEntity.CODE_FUND).catalogName("基金")
                .catalogScope(AssetCatalogEntity.SCOPE_SYSTEM).balanceDirection(AssetCatalogEntity.DIRECTION_ADD)
                .status(AssetCatalogEntity.STATUS_ENABLED).build();
        private AssetRecordEntity record;
        private int childCount;
        private long nextId = 101L;
        private final List<AssetRecordChangeEntity> changes = new ArrayList<>();

        @Override public AssetCatalogEntity queryVisibleCatalog(Long userId, Long catalogId) { return catalog; }
        @Override public int countEnabledChildren(Long userId, Long catalogId) { return childCount; }
        @Override public void insertRecord(AssetRecordEntity value) { value.assignId(nextId++); record = value; }
        @Override public AssetRecordEntity queryRecordForUpdate(Long userId, Long recordId) { return record; }
        @Override public void updateRecord(AssetRecordEntity value) { record = value; }
        @Override public void insertRecordChanges(List<AssetRecordChangeEntity> values) { changes.addAll(values); }
        @Override public List<com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity> queryCurrentHoldings(Long userId) { return List.of(); }
        @Override public void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets) { }
        @Override public WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind) { return null; }
    }

    private static class FakeFundRepository implements IFundDataRepository {
        @Override
        public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) {
            return Map.of("000001", FundCurrentDataAggregate.FundDetail.builder()
                    .id(88L).fundCode("000001").fundName("公开示例基金").build());
        }
        @Override public Set<String> queryExistingFundCodes(Collection<String> fundCodes) { return Set.of("000001"); }
        @Override public void upsertCatalogs(List<FundCurrentDataAggregate.FundDetail> funds) { }
    }

    private static class FakeStockRepository implements IStockMarketRepository {
        private StockMarketEntity stock;
        @Override public void registerQuoteTargets(List<StockMarketEntity> quoteTargets) { }
        @Override public void upsertMarkets(List<StockMarketEntity> markets) { }
        @Override public Map<String, StockMarketEntity> queryByStockKeys(Collection<String> stockKeys) { return Map.of(); }
        @Override public Set<String> queryExistingStockKeys(Collection<String> stockKeys) { return Set.of(); }
        @Override public StockMarketEntity queryOne(String stockCode, String market) { return stock; }
    }
}
