package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IAssetAccountDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IAssetHoldingDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IAssetInfoDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetAccountPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetHoldingPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetInfoPO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.List;

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

}
