package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IAssetAccountDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IAssetHoldingDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IAssetInfoDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetAccountPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetHoldingPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetInfoPO;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Repository
public class PortfolioRepository implements IPortfolioRepository {

    @Resource
    private IAssetHoldingDao assetHoldingDao;

    @Resource
    private IAssetAccountDao assetAccountDao;

    @Resource
    private IAssetInfoDao assetInfoDao;

    @Override
    public List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId) {
        return assetHoldingDao.selectByUserId(userId).stream()
                .filter(holding -> "active".equals(holding.getStatus()))
                .map(this::toEntity)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets) {
        if (watchlistAssets == null || watchlistAssets.isEmpty()) {
            return;
        }
        for (WatchlistAssetEntity watchlistAsset : watchlistAssets) {
            assetInfoDao.upsertWatchlistAsset(toAssetInfoPO(watchlistAsset));
        }
    }

    @Override
    public WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind) {
        AssetInfoPO assetInfoPO = assetInfoDao.selectByUserIdAndAssetCodeAndAssetKind(
                userId, assetCode, assetKind);
        return assetInfoPO == null ? null : toWatchlistAssetEntity(assetInfoPO);
    }

    @Override
    public List<WatchlistAssetEntity> queryWatchlistAssets(Long userId, String assetKind) {
        return assetInfoDao.selectEnabledByUserId(userId, assetKind).stream()
                .map(this::toWatchlistAssetEntity)
                .toList();
    }

    @Override
    public Set<String> queryWatchlistedIdentityKeys(Long userId, Collection<String> identityKeys) {
        if (identityKeys == null || identityKeys.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (AssetInfoPO po : assetInfoDao.selectWatchlistedByIdentities(userId, identityKeys)) {
            result.add(po.getAssetKind() + "#" + po.getAssetCode());
        }
        return result;
    }

    private PortfolioHoldingEntity toEntity(AssetHoldingPO holdingPO) {
        AssetAccountPO accountPO = assetAccountDao.selectById(holdingPO.getAccountId());
        AssetInfoPO assetInfoPO = assetInfoDao.selectById(holdingPO.getAssetId());
        return PortfolioHoldingEntity.builder()
                .userId(holdingPO.getUserId())
                .holdingId(holdingPO.getId())
                .accountId(holdingPO.getAccountId())
                .accountName(accountPO == null ? null : accountPO.getAccountName())
                .accountType(accountPO == null ? null : accountPO.getAccountType())
                .assetId(holdingPO.getAssetId())
                .assetCode(assetInfoPO == null ? null : assetInfoPO.getAssetCode())
                .assetName(assetInfoPO == null ? null : assetInfoPO.getAssetName())
                .assetKind(assetInfoPO == null ? null : assetInfoPO.getAssetKind())
                .assetType(assetInfoPO == null ? null : assetInfoPO.getAssetType())
                .assetCategory(holdingPO.getAssetCategory())
                .holdingSource(holdingPO.getHoldingSource())
                .amount(holdingPO.getAmount())
                .currency(holdingPO.getCurrency())
                .amountDisplay(holdingPO.getAmountDisplay())
                .amountMissingReason(holdingPO.getAmountMissingReason())
                .missingReasonsJson(holdingPO.getMissingReasonsJson())
                .status(holdingPO.getStatus())
                .build();
    }

    private AssetInfoPO toAssetInfoPO(WatchlistAssetEntity watchAsset) {
        return AssetInfoPO.builder()
                .id(watchAsset.getId())
                .userId(watchAsset.getUserId())
                .assetCode(watchAsset.getAssetCode())
                .assetName(watchAsset.getAssetName())
                .assetKind(watchAsset.getAssetKind())
                .assetType(watchAsset.getAssetType())
                .market(watchAsset.getMarket())
                .status(watchAsset.getStatus())
                .build();
    }

    private WatchlistAssetEntity toWatchlistAssetEntity(AssetInfoPO assetInfoPO) {
        return WatchlistAssetEntity.builder()
                .id(assetInfoPO.getId())
                .userId(assetInfoPO.getUserId())
                .assetCode(assetInfoPO.getAssetCode())
                .assetName(assetInfoPO.getAssetName())
                .assetKind(assetInfoPO.getAssetKind())
                .assetType(assetInfoPO.getAssetType())
                .market(assetInfoPO.getMarket())
                .status(assetInfoPO.getStatus())
                .build();
    }

}
