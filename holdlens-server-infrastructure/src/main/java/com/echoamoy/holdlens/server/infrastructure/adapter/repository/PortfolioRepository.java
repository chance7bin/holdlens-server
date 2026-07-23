package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordChangeEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.ExchangeRateEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.PortfolioHoldingEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IAssetCatalogDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IAssetRecordChangeDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IAssetRecordDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IExchangeRateDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IFundDao;
import com.echoamoy.holdlens.server.infrastructure.dao.IWatchlistItemDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetCatalogPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetRecordChangePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.AssetRecordPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.ExchangeRatePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.WatchlistItemPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Repository
public class PortfolioRepository implements IPortfolioRepository {

    @Resource private IAssetCatalogDao assetCatalogDao;
    @Resource private IAssetRecordDao assetRecordDao;
    @Resource private IAssetRecordChangeDao assetRecordChangeDao;
    @Resource private IWatchlistItemDao watchlistItemDao;
    @Resource private IExchangeRateDao exchangeRateDao;
    @Resource private IFundDao fundDao;

    @Override
    public List<AssetCatalogEntity> queryVisibleCatalogs(Long userId) {
        return assetCatalogDao.selectVisible(userId).stream().map(this::toCatalogEntity).toList();
    }

    @Override
    public AssetCatalogEntity queryVisibleCatalog(Long userId, Long catalogId) {
        AssetCatalogPO po = assetCatalogDao.selectVisibleById(userId, catalogId);
        return po == null ? null : toCatalogEntity(po);
    }

    @Override
    public AssetCatalogEntity queryCatalogByCode(String catalogCode) {
        AssetCatalogPO po = assetCatalogDao.selectByCode(catalogCode);
        return po == null ? null : toCatalogEntity(po);
    }

    @Override
    public int countEnabledChildren(Long userId, Long catalogId) {
        return assetCatalogDao.countEnabledChildren(userId, catalogId);
    }

    @Override
    public int countActiveRecords(Long userId, Long catalogId) {
        return assetRecordDao.countActiveByCatalog(userId, catalogId);
    }

    @Override
    public void insertCatalog(AssetCatalogEntity catalog) {
        AssetCatalogPO po = toCatalogPO(catalog);
        assetCatalogDao.insert(po);
        catalog.assignId(po.getId());
    }

    @Override
    public void updateCatalog(AssetCatalogEntity catalog) {
        if (assetCatalogDao.updateUserCatalog(toCatalogPO(catalog)) != 1) {
            throw new IllegalStateException("资产目录更新失败");
        }
    }

    @Override
    public void insertRecord(AssetRecordEntity record) {
        AssetRecordPO po = toRecordPO(record);
        assetRecordDao.insert(po);
        record.assignId(po.getId());
    }

    @Override
    public AssetRecordEntity queryRecord(Long userId, Long recordId) {
        AssetRecordPO po = assetRecordDao.selectByUserAndId(userId, recordId);
        return po == null ? null : toRecordEntity(po);
    }

    @Override
    public AssetRecordEntity queryRecordForUpdate(Long userId, Long recordId) {
        AssetRecordPO po = assetRecordDao.selectByUserAndIdForUpdate(userId, recordId);
        return po == null ? null : toRecordEntity(po);
    }

    @Override
    public List<AssetRecordEntity> queryActiveRecords(Long userId) {
        return assetRecordDao.selectActiveByUserId(userId).stream().map(this::toRecordEntity).toList();
    }

    @Override
    public void updateRecord(AssetRecordEntity record) {
        if (assetRecordDao.updateMutable(toRecordPO(record)) != 1) {
            throw new IllegalStateException("资产记录更新失败");
        }
    }

    @Override
    public void insertRecordChanges(List<AssetRecordChangeEntity> changes) {
        if (changes != null && !changes.isEmpty()) {
            assetRecordChangeDao.insertBatch(changes.stream().map(this::toChangePO).toList());
        }
    }

    @Override
    public void upsertExchangeRate(ExchangeRateEntity rate) {
        exchangeRateDao.upsert(toExchangeRatePO(rate));
    }

    @Override
    public ExchangeRateEntity queryExchangeRate(String baseCurrency, String quoteCurrency) {
        ExchangeRatePO po = exchangeRateDao.selectOne(baseCurrency, quoteCurrency);
        return po == null ? null : toExchangeRateEntity(po);
    }

    @Override
    public List<ExchangeRateEntity> queryExchangeRates(Collection<String> baseCurrencies, String quoteCurrency) {
        if (baseCurrencies == null || baseCurrencies.isEmpty()) return List.of();
        return exchangeRateDao.selectByBases(baseCurrencies, quoteCurrency).stream()
                .map(this::toExchangeRateEntity).toList();
    }

    @Override
    public List<PortfolioHoldingEntity> queryCurrentHoldings(Long userId) {
        return queryActiveRecords(userId).stream()
                .filter(record -> AssetRecordEntity.KIND_FUND.equals(record.getAssetKind()) && record.getAssetId() != null)
                .map(this::toFundHolding)
                .filter(holding -> holding.getAssetCode() != null)
                .toList();
    }

    @Override
    public void upsertWatchlistAssets(List<WatchlistAssetEntity> watchlistAssets) {
        if (watchlistAssets == null) return;
        for (WatchlistAssetEntity asset : watchlistAssets) {
            if (asset.getAssetId() == null || asset.getAssetId() <= 0) {
                throw new IllegalArgumentException("自选公共资产ID不合法");
            }
            watchlistItemDao.upsert(WatchlistItemPO.builder()
                    .userId(asset.getUserId()).assetKind(toInternalKind(asset.getAssetKind())).assetId(asset.getAssetId()).build());
        }
    }

    @Override
    public void deleteWatchlistAsset(Long userId, String assetKind, Long assetId) {
        watchlistItemDao.delete(userId, toInternalKind(assetKind), assetId);
    }

    @Override
    public WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind) {
        return queryWatchlistAsset(userId, assetCode, assetKind, null);
    }

    @Override
    public WatchlistAssetEntity queryWatchlistAsset(Long userId, String assetCode, String assetKind, String market) {
        WatchlistItemPO po = watchlistItemDao.selectOneByPublicIdentity(
                userId, toInternalKind(assetKind), assetCode, market);
        return po == null ? null : toWatchlistEntity(po);
    }

    @Override
    public List<WatchlistAssetEntity> queryWatchlistAssets(Long userId, String assetKind) {
        String internalKind = assetKind == null || assetKind.isBlank() ? null : toInternalKind(assetKind);
        return watchlistItemDao.selectByUser(userId, internalKind).stream().map(this::toWatchlistEntity).toList();
    }

    @Override
    public Set<String> queryWatchlistedIdentityKeys(Long userId, Collection<String> identityKeys) {
        if (identityKeys == null || identityKeys.isEmpty()) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        for (WatchlistItemPO po : watchlistItemDao.selectByPublicIdentities(userId, identityKeys)) {
            String kind = toExternalKind(po.getAssetKind());
            result.add(kind + "#" + po.getAssetCode()
                    + ("stock".equals(kind) ? "#" + po.getMarket() : ""));
        }
        return result;
    }

    private PortfolioHoldingEntity toFundHolding(AssetRecordEntity record) {
        FundPO fund = fundDao.selectById(record.getAssetId());
        return PortfolioHoldingEntity.builder()
                .userId(record.getUserId()).recordId(record.getId()).assetId(record.getAssetId())
                .assetRef(fund == null ? null : "fund:" + fund.getFundCode())
                .assetCode(fund == null ? null : fund.getFundCode())
                .assetName(record.getRecordName()).assetKind("fund")
                .assetType(fund == null ? null : fund.getFundType())
                .amount(record.getAmount()).currency(record.getCurrency()).status(record.getStatus().toLowerCase(Locale.ROOT))
                .build();
    }

    private AssetCatalogEntity toCatalogEntity(AssetCatalogPO po) {
        return AssetCatalogEntity.builder().id(po.getId()).userId(po.getUserId()).parentId(po.getParentId())
                .catalogCode(po.getCatalogCode()).catalogName(po.getCatalogName()).catalogScope(po.getCatalogScope())
                .balanceDirection(po.getBalanceDirection()).sortOrder(po.getSortOrder()).status(po.getStatus())
                .createTime(toLocal(po.getCreateTime())).updateTime(toLocal(po.getUpdateTime())).build();
    }

    private AssetCatalogPO toCatalogPO(AssetCatalogEntity entity) {
        return AssetCatalogPO.builder().id(entity.getId()).userId(entity.getUserId()).parentId(entity.getParentId())
                .catalogCode(entity.getCatalogCode()).catalogName(entity.getCatalogName()).catalogScope(entity.getCatalogScope())
                .balanceDirection(entity.getBalanceDirection()).sortOrder(entity.getSortOrder()).status(entity.getStatus()).build();
    }

    private AssetRecordEntity toRecordEntity(AssetRecordPO po) {
        return AssetRecordEntity.builder().id(po.getId()).userId(po.getUserId()).catalogId(po.getCatalogId())
                .catalogCode(po.getCatalogCode()).balanceDirection(po.getBalanceDirection()).recordName(po.getRecordName())
                .assetKind(po.getAssetKind()).assetId(po.getAssetId()).assetRef(po.getAssetRef())
                .amount(po.getAmount()).currency(po.getCurrency())
                .remark(po.getRemark()).status(po.getStatus()).createTime(toLocal(po.getCreateTime()))
                .updateTime(toLocal(po.getUpdateTime())).build();
    }

    private AssetRecordPO toRecordPO(AssetRecordEntity entity) {
        return AssetRecordPO.builder().id(entity.getId()).userId(entity.getUserId()).catalogId(entity.getCatalogId())
                .recordName(entity.getRecordName()).assetKind(entity.getAssetKind()).assetId(entity.getAssetId())
                .amount(entity.getAmount()).currency(entity.getCurrency()).remark(entity.getRemark()).status(entity.getStatus()).build();
    }

    private AssetRecordChangePO toChangePO(AssetRecordChangeEntity entity) {
        return AssetRecordChangePO.builder().operationId(entity.getOperationId()).userId(entity.getUserId())
                .recordId(entity.getRecordId()).changeType(entity.getChangeType()).beforeAmount(entity.getBeforeAmount())
                .afterAmount(entity.getAfterAmount()).currency(entity.getCurrency()).beforeStatus(entity.getBeforeStatus())
                .afterStatus(entity.getAfterStatus()).operatorId(entity.getOperatorId()).build();
    }

    private WatchlistAssetEntity toWatchlistEntity(WatchlistItemPO po) {
        return WatchlistAssetEntity.builder().id(po.getId()).userId(po.getUserId()).assetId(po.getAssetId())
                .assetCode(po.getAssetCode()).assetName(po.getAssetName()).assetKind(toExternalKind(po.getAssetKind()))
                .assetType(po.getAssetType()).market(po.getMarket()).build();
    }

    private ExchangeRatePO toExchangeRatePO(ExchangeRateEntity entity) {
        return ExchangeRatePO.builder().id(entity.getId()).baseCurrency(entity.getBaseCurrency())
                .quoteCurrency(entity.getQuoteCurrency()).rate(entity.getRate()).source(entity.getSource())
                .sourceAsOf(toDate(entity.getSourceAsOf())).fetchedAt(toDate(entity.getFetchedAt())).build();
    }

    private ExchangeRateEntity toExchangeRateEntity(ExchangeRatePO po) {
        return ExchangeRateEntity.builder().id(po.getId()).baseCurrency(po.getBaseCurrency())
                .quoteCurrency(po.getQuoteCurrency()).rate(po.getRate()).source(po.getSource())
                .sourceAsOf(toLocal(po.getSourceAsOf())).fetchedAt(toLocal(po.getFetchedAt()))
                .createTime(toLocal(po.getCreateTime())).updateTime(toLocal(po.getUpdateTime())).build();
    }

    private static String toInternalKind(String kind) {
        if (kind == null) throw new IllegalArgumentException("资产类型不能为空");
        String value = kind.trim().toUpperCase(Locale.ROOT);
        if (!AssetRecordEntity.KIND_FUND.equals(value) && !AssetRecordEntity.KIND_STOCK.equals(value)) {
            throw new IllegalArgumentException("资产类型不支持");
        }
        return value;
    }

    private static String toExternalKind(String kind) {
        return kind == null ? null : kind.toLowerCase(Locale.ROOT);
    }

    private static LocalDateTime toLocal(java.util.Date value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

    private static java.util.Date toDate(LocalDateTime value) {
        return value == null ? null : java.util.Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

}
