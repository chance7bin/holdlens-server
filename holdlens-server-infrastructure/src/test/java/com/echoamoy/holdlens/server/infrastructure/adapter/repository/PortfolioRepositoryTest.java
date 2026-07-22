package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IAssetInfoDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetInfoPO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PortfolioRepositoryTest {

    @Test
    public void upsertWatchlistAssetsMapsWatchlistAssetToAssetInfo() throws Exception {
        PortfolioRepository repository = new PortfolioRepository();
        FakeAssetInfoDao assetInfoDao = new FakeAssetInfoDao();
        setField(repository, "assetInfoDao", assetInfoDao);

        repository.upsertWatchlistAssets(List.of(WatchlistAssetEntity.builder()
                .userId(1001L)
                .assetCode("000001")
                .assetKind("fund")
                .assetName("测试基金")
                .market(null)
                .status("enabled")
                .build()));

        AssetInfoPO po = assetInfoDao.upserted.get(0);
        Assert.assertEquals(Long.valueOf(1001L), po.getUserId());
        Assert.assertEquals("000001", po.getAssetCode());
        Assert.assertEquals("fund", po.getAssetKind());
        Assert.assertEquals("测试基金", po.getAssetName());
        Assert.assertNull(po.getMarket());
    }

    @Test
    public void queryWatchlistAssetUsesExistingAssetInfoIdentityWithoutChangingTableStructure() throws Exception {
        PortfolioRepository repository = new PortfolioRepository();
        FakeAssetInfoDao assetInfoDao = new FakeAssetInfoDao();
        assetInfoDao.selected = AssetInfoPO.builder()
                .id(1L)
                .userId(1001L)
                .assetCode("600000")
                .assetKind("stock")
                .market(null)
                .build();
        setField(repository, "assetInfoDao", assetInfoDao);

        WatchlistAssetEntity entity = repository.queryWatchlistAsset(1001L, "600000", "stock");

        Assert.assertEquals("600000", entity.getAssetCode());
        Assert.assertEquals(Long.valueOf(1001L), assetInfoDao.userId);
        Assert.assertEquals("600000", assetInfoDao.assetCode);
        Assert.assertEquals("stock", assetInfoDao.assetKind);

        repository.queryWatchlistAsset(1001L, "600000", "stock");
        Assert.assertEquals("stock", assetInfoDao.assetKind);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeAssetInfoDao implements IAssetInfoDao {
        private final List<AssetInfoPO> upserted = new ArrayList<>();
        private AssetInfoPO selected;
        private Long userId;
        private String assetCode;
        private String assetKind;

        @Override
        public void upsertWatchlistAsset(AssetInfoPO assetInfoPO) {
            upserted.add(assetInfoPO);
        }

        @Override
        public AssetInfoPO selectById(Long id) {
            return null;
        }

        @Override
        public List<AssetInfoPO> selectByUserId(Long userId) {
            return List.of();
        }

        @Override
        public AssetInfoPO selectByUserIdAndAssetCodeAndAssetKind(Long userId, String assetCode, String assetKind) {
            this.userId = userId;
            this.assetCode = assetCode;
            this.assetKind = assetKind;
            return selected;
        }

        @Override
        public List<AssetInfoPO> selectByUserIdAndAssetName(Long userId, String assetName) {
            return List.of();
        }

        @Override
        public List<AssetInfoPO> selectEnabledByUserId(Long userId, String assetKind) {
            return List.of();
        }

        @Override
        public List<AssetInfoPO> selectWatchlistedByIdentities(Long userId, Collection<String> identityKeys) {
            return List.of();
        }
    }

}
