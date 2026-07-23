package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IWatchlistItemDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.WatchlistItemPO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PortfolioRepositoryTest {

    @Test
    public void upsertWatchlistAssetsPersistsOnlyInternalPublicAssetReference() throws Exception {
        PortfolioRepository repository = new PortfolioRepository();
        FakeWatchlistItemDao dao = new FakeWatchlistItemDao();
        setField(repository, "watchlistItemDao", dao);

        repository.upsertWatchlistAssets(List.of(WatchlistAssetEntity.builder()
                .userId(1001L).assetId(88L).assetCode("000001").assetKind("fund")
                .assetName("测试基金").build()));

        WatchlistItemPO po = dao.upserted.get(0);
        Assert.assertEquals(Long.valueOf(1001L), po.getUserId());
        Assert.assertEquals(Long.valueOf(88L), po.getAssetId());
        Assert.assertEquals("FUND", po.getAssetKind());
        Assert.assertNull(po.getAssetCode());
        Assert.assertNull(po.getAssetName());
    }

    @Test
    public void queryStockWatchlistIdentityIncludesMarket() throws Exception {
        PortfolioRepository repository = new PortfolioRepository();
        FakeWatchlistItemDao dao = new FakeWatchlistItemDao();
        dao.selected = WatchlistItemPO.builder().id(1L).userId(1001L).assetId(9L)
                .assetCode("DEMO").assetName("示例股票").assetKind("STOCK").market("US_STOCK").build();
        setField(repository, "watchlistItemDao", dao);

        WatchlistAssetEntity entity = repository.queryWatchlistAsset(1001L, "DEMO", "stock", "US_STOCK");

        Assert.assertEquals(Long.valueOf(9L), entity.getAssetId());
        Assert.assertEquals("stock", entity.getAssetKind());
        Assert.assertEquals("US_STOCK", dao.market);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeWatchlistItemDao implements IWatchlistItemDao {
        private final List<WatchlistItemPO> upserted = new ArrayList<>();
        private WatchlistItemPO selected;
        private String market;

        @Override public void upsert(WatchlistItemPO item) { upserted.add(item); }
        @Override public int delete(Long userId, String assetKind, Long assetId) { return 0; }
        @Override
        public WatchlistItemPO selectOneByPublicIdentity(Long userId, String assetKind, String assetCode, String market) {
            this.market = market;
            return selected;
        }
        @Override public List<WatchlistItemPO> selectByUser(Long userId, String assetKind) { return List.of(); }
        @Override
        public List<WatchlistItemPO> selectByPublicIdentities(Long userId, Collection<String> identityKeys) {
            return List.of();
        }
    }
}
