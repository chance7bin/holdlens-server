package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddCommand;
import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WatchlistAssetBatchAddCaseImplTest {

    @Test
    public void batchAddAcceptsAssetRefAndRejectsConflictingLegacyFields() throws Exception {
        WatchlistAssetBatchAddCaseImpl batchAddCase = newCase(
                new FakeFundDataRepository(Set.of("000001")),
                new FakeStockMarketRepository(Set.of("DEMO#US_STOCK")));

        WatchlistAssetBatchAddResult result = batchAddCase.batchAdd(WatchlistAssetBatchAddCommand.builder()
                .userId(1001L)
                .items(List.of(
                        WatchlistAssetBatchAddCommand.Item.builder().assetKind("stock")
                                .assetRef("stock:US_STOCK:DEMO").build(),
                        WatchlistAssetBatchAddCommand.Item.builder().assetKind("fund")
                                .assetRef("fund:000001").assetCode("999999").build()))
                .build());

        FakePortfolioRepository portfolioRepository = getField(batchAddCase, "portfolioRepository");
        Assert.assertEquals(1, portfolioRepository.watchlistAssets.size());
        Assert.assertEquals("US_STOCK", portfolioRepository.watchlistAssets.get(0).getMarket());
        Assert.assertEquals(1, result.getInvalidItems().size());
        Assert.assertEquals("fund:000001", result.getInvalidItems().get(0).getAssetRef());
        Assert.assertEquals("ASSET_REF_CONFLICT", result.getInvalidItems().get(0).getReasonCode());
    }

    @Test
    public void batchAddDeduplicatesValidatesExistingPublicAssetsAndReturnsOnlyInvalidItems() throws Exception {
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository(Set.of("000001"));
        FakeStockMarketRepository stockMarketRepository = new FakeStockMarketRepository(Set.of("600000#", "000001#SH"));
        WatchlistAssetBatchAddCaseImpl batchAddCase = newCase(fundDataRepository, stockMarketRepository);

        WatchlistAssetBatchAddResult result = batchAddCase.batchAdd(WatchlistAssetBatchAddCommand.builder()
                .userId(1001L)
                .items(List.of(
                        item("fund", "000001", null, null),
                        item("stock", "600000", null, null),
                        item("stock", "600000", null, null),
                        item("stock", "000001", "测试股票", "SH"),
                        item("stock", "000001", "测试股票2", "SZ"),
                        item("fund", "999999", null, null),
                        item("stock", "300000", null, null),
                        item("bond", "123456", null, null),
                        item("fund", " ", null, null)))
                .build());

        FakePortfolioRepository portfolioRepository = getField(batchAddCase, "portfolioRepository");

        Assert.assertEquals(result.getInvalidItems().toString(), 4, result.getInvalidItems().size());
        Assert.assertEquals(Integer.valueOf(5), result.getInvalidItems().get(0).getIndex());
        Assert.assertEquals("FUND_NOT_FOUND", result.getInvalidItems().get(0).getReasonCode());
        Assert.assertEquals(Integer.valueOf(6), result.getInvalidItems().get(1).getIndex());
        Assert.assertEquals("STOCK_NOT_FOUND", result.getInvalidItems().get(1).getReasonCode());
        Assert.assertEquals(Integer.valueOf(7), result.getInvalidItems().get(2).getIndex());
        Assert.assertEquals("ASSET_KIND_UNSUPPORTED", result.getInvalidItems().get(2).getReasonCode());
        Assert.assertEquals(Integer.valueOf(8), result.getInvalidItems().get(3).getIndex());
        Assert.assertEquals("ASSET_CODE_REQUIRED", result.getInvalidItems().get(3).getReasonCode());

        Assert.assertEquals(3, portfolioRepository.watchlistAssets.size());
        Assert.assertEquals("公开基金000001", portfolioRepository.watchlistAssets.get(0).getAssetName());
        Assert.assertEquals("公开股票600000", portfolioRepository.watchlistAssets.get(1).getAssetName());
        Assert.assertEquals("公开股票000001", portfolioRepository.watchlistAssets.get(2).getAssetName());
        Assert.assertNull(portfolioRepository.watchlistAssets.get(1).getMarket());
        Assert.assertTrue(stockMarketRepository.registeredQuoteTargets.isEmpty());
    }

    @Test
    public void batchAddDoesNotTriggerRefreshSideEffects() throws Exception {
        WatchlistAssetBatchAddCaseImpl batchAddCase = newCase(
                new FakeFundDataRepository(Set.of("000001")),
                new FakeStockMarketRepository(Set.of()));

        WatchlistAssetBatchAddResult result = batchAddCase.batchAdd(WatchlistAssetBatchAddCommand.builder()
                .userId(1001L)
                .items(List.of(item("fund", "000001", null, null)))
                .build());

        FakePortfolioRepository portfolioRepository = getField(batchAddCase, "portfolioRepository");
        FakeFundDataRepository fundDataRepository = getField(batchAddCase, "fundDataRepository");
        FakeStockMarketRepository stockMarketRepository = getField(batchAddCase, "stockMarketRepository");

        Assert.assertTrue(result.getInvalidItems().isEmpty());
        Assert.assertEquals(1, portfolioRepository.watchlistAssets.size());
        Assert.assertTrue(stockMarketRepository.registeredQuoteTargets.isEmpty());
    }

    private WatchlistAssetBatchAddCommand.Item item(String assetKind, String assetCode, String assetName, String market) {
        return WatchlistAssetBatchAddCommand.Item.builder()
                .assetKind(assetKind)
                .assetCode(assetCode)
                .assetName(assetName)
                .market(market)
                .build();
    }

    private WatchlistAssetBatchAddCaseImpl newCase(FakeFundDataRepository fundDataRepository,
                                                   FakeStockMarketRepository stockMarketRepository) throws Exception {
        WatchlistAssetBatchAddCaseImpl batchAddCase = new WatchlistAssetBatchAddCaseImpl();
        setField(batchAddCase, "portfolioRepository", new FakePortfolioRepository());
        setField(batchAddCase, "fundDataRepository", fundDataRepository);
        setField(batchAddCase, "stockMarketRepository", stockMarketRepository);
        return batchAddCase;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static class FakePortfolioRepository implements IPortfolioRepository {
        private final List<WatchlistAssetEntity> watchlistAssets = new ArrayList<>();

        @Override
        public List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId) {
            return List.of();
        }

        @Override
        public void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets) {
            this.watchlistAssets.addAll(watchlistAssets);
        }

        @Override
        public WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind) {
            return null;
        }
    }

    private static class FakeFundDataRepository implements IFundDataRepository {
        private final Set<String> existingFundCodes;

        private FakeFundDataRepository(Set<String> existingFundCodes) {
            this.existingFundCodes = existingFundCodes;
        }

        @Override
        public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) {
            java.util.Map<String, FundCurrentDataAggregate.FundDetail> result = new java.util.LinkedHashMap<>();
            for (String fundCode : fundCodes) {
                if (existingFundCodes.contains(fundCode)) {
                    result.put(fundCode, FundCurrentDataAggregate.FundDetail.builder()
                            .fundCode(fundCode)
                            .fundName("公开基金" + fundCode)
                            .build());
                }
            }
            return result;
        }

        @Override
        public Set<String> queryExistingFundCodes(Collection<String> fundCodes) {
            Set<String> result = new LinkedHashSet<>();
            for (String fundCode : fundCodes) {
                if (existingFundCodes.contains(fundCode)) {
                    result.add(fundCode);
                }
            }
            return result;
        }

        @Override
        public void upsertCatalogs(List<FundCurrentDataAggregate.FundDetail> funds) {
            throw new UnsupportedOperationException();
        }

    }

    private static class FakeStockMarketRepository implements IStockMarketRepository {
        private final Set<String> existingStockKeys;
        private final List<StockMarketEntity> registeredQuoteTargets = new ArrayList<>();

        private FakeStockMarketRepository(Set<String> existingStockKeys) {
            this.existingStockKeys = existingStockKeys;
        }

        @Override
        public void registerQuoteTargets(List<StockMarketEntity> quoteTargets) {
            registeredQuoteTargets.addAll(quoteTargets);
        }

        @Override
        public void upsertMarkets(List<StockMarketEntity> markets) {
        }

        @Override
        public Map<String, StockMarketEntity> queryByStockKeys(Collection<String> stockKeys) {
            java.util.Map<String, StockMarketEntity> result = new java.util.LinkedHashMap<>();
            for (String stockKey : stockKeys) {
                if (existingStockKeys.contains(stockKey)) {
                    String[] parts = stockKey.split("#", -1);
                    result.put(stockKey, StockMarketEntity.builder()
                            .stockCode(parts[0])
                            .market(parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null)
                            .stockName("公开股票" + parts[0])
                            .build());
                }
            }
            return result;
        }

        @Override
        public Set<String> queryExistingStockKeys(Collection<String> stockKeys) {
            Set<String> result = new LinkedHashSet<>();
            for (String stockKey : stockKeys) {
                if (existingStockKeys.contains(stockKey)) {
                    result.add(stockKey);
                }
            }
            return result;
        }
    }

}
