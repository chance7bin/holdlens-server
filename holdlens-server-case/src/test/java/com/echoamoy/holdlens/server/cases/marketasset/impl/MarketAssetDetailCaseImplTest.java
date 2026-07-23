package com.echoamoy.holdlens.server.cases.marketasset.impl;

import com.echoamoy.holdlens.server.cases.marketasset.IMarketAssetQueryCase;
import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetDetailResult;
import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetQueryResult;
import com.echoamoy.holdlens.server.cases.portfolio.IPortfolioFundDetailCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.PortfolioFundDetailResult;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.types.exception.AppException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

public class MarketAssetDetailCaseImplTest {

    private MarketAssetDetailCaseImpl detailCase;
    private FakePortfolio portfolio;

    @Before
    public void setUp() throws Exception {
        detailCase = new MarketAssetDetailCaseImpl();
        portfolio = new FakePortfolio();
        set(detailCase, "portfolioRepository", portfolio);
        set(detailCase, "portfolioFundDetailCase", new FakeFundDetailCase());
        set(detailCase, "marketAssetQueryCase", new FakeMarketAssetQueryCase());
    }

    @Test
    public void queriesFundByOpaqueReferenceWithoutPortfolioRecords() {
        MarketAssetDetailResult result = detailCase.queryDetail(1L, "fund", "fund:000001");

        Assert.assertEquals("000001", result.getFund().getFundCode());
        Assert.assertTrue(result.getWatchlisted());
        Assert.assertNull(result.getStock());
        Assert.assertEquals(0, portfolio.assetRecordQueryCount);
    }

    @Test
    public void queriesStockByOpaqueReference() {
        MarketAssetDetailResult result = detailCase.queryDetail(1L, "stock", "stock:US_STOCK:DEMO");

        Assert.assertEquals("DEMO", result.getStock().getCode());
        Assert.assertNull(result.getFund());
    }

    @Test(expected = AppException.class)
    public void rejectsKindAndReferenceConflict() {
        detailCase.queryDetail(1L, "fund", "stock:US_STOCK:DEMO");
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakePortfolio implements IPortfolioRepository {
        private int assetRecordQueryCount;
        @Override public List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId) {
            assetRecordQueryCount++;
            return List.of();
        }
        @Override public void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets) { }
        @Override public WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind) {
            return WatchlistAssetEntity.builder().userId(userId).assetCode(assetCode).assetKind(assetKind).build();
        }
    }

    private static class FakeFundDetailCase implements IPortfolioFundDetailCase {
        @Override public PortfolioFundDetailResult queryPortfolioFundDetails(Long userId) { return null; }
        @Override public PortfolioFundDetailResult.FundDetail queryFundDetail(String fundCode) {
            return PortfolioFundDetailResult.FundDetail.builder().fundCode(fundCode).fundName("示例基金").build();
        }
    }

    private static class FakeMarketAssetQueryCase implements IMarketAssetQueryCase {
        @Override public MarketAssetQueryResult.Watchlist queryWatchlist(Long userId, String assetKind) { return null; }
        @Override public MarketAssetQueryResult.Search search(Long userId, String keyword, String assetKind,
                                                               String market, Integer limit) { return null; }
        @Override public MarketAssetQueryResult.StockDetail queryStockDetail(Long userId, String assetRef) {
            return MarketAssetQueryResult.StockDetail.builder().assetKind("stock").assetRef(assetRef)
                    .code("DEMO").market("US_STOCK").watchlisted(false).build();
        }
    }
}
