package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.IAssetManagementCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.AssetManagementCommand;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.marketasset.model.valobj.MarketAssetRefVO;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetCatalogEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordChangeEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetRecordEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.AssetSummaryEntity;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.ExchangeRateEntity;
import com.echoamoy.holdlens.server.domain.portfolio.service.AssetSummaryService;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AssetManagementCaseImpl implements IAssetManagementCase {

    @Resource private IPortfolioRepository portfolioRepository;
    @Resource private IFundDataRepository fundDataRepository;
    @Resource private IStockMarketRepository stockMarketRepository;

    private final AssetSummaryService assetSummaryService = new AssetSummaryService();

    @Override
    public List<AssetCatalogEntity> queryCatalogs(Long userId) {
        requireUserId(userId);
        return portfolioRepository.queryVisibleCatalogs(userId);
    }

    @Override
    @Transactional
    public AssetCatalogEntity createCatalog(AssetManagementCommand.CreateCatalog command) {
        if (command == null) throw new IllegalArgumentException("目录创建参数不能为空");
        requireUserId(command.getUserId());
        validateParent(command.getUserId(), command.getParentId(), command.getBalanceDirection(), null, false);
        AssetCatalogEntity catalog = AssetCatalogEntity.createUser(command.getUserId(), command.getParentId(),
                command.getCatalogName(), command.getBalanceDirection(), command.getSortOrder());
        portfolioRepository.insertCatalog(catalog);
        return catalog;
    }

    @Override
    @Transactional
    public AssetCatalogEntity updateCatalog(AssetManagementCommand.UpdateCatalog command) {
        if (command == null) throw new IllegalArgumentException("目录更新参数不能为空");
        AssetCatalogEntity catalog = requireCatalog(command.getUserId(), command.getCatalogId());
        if (catalog.isSystem() || !command.getUserId().equals(catalog.getUserId())) {
            throw new IllegalStateException("系统目录或其他用户目录不可修改");
        }
        if (command.getParentId() != null && command.getParentId().equals(catalog.getId())) {
            throw new IllegalArgumentException("目录不能成为自己的子目录");
        }
        boolean hasChildren = portfolioRepository.countEnabledChildren(command.getUserId(), catalog.getId()) > 0;
        validateParent(command.getUserId(), command.getParentId(), command.getBalanceDirection(), catalog.getId(), hasChildren);
        catalog.updateUserCatalog(command.getParentId(), command.getCatalogName(),
                command.getBalanceDirection(), command.getSortOrder());
        portfolioRepository.updateCatalog(catalog);
        return catalog;
    }

    @Override
    @Transactional
    public void deleteCatalog(Long userId, Long catalogId) {
        AssetCatalogEntity catalog = requireCatalog(userId, catalogId);
        if (catalog.isSystem() || !userId.equals(catalog.getUserId())) {
            throw new IllegalStateException("系统目录或其他用户目录不可删除");
        }
        if (portfolioRepository.countEnabledChildren(userId, catalogId) > 0) {
            throw new IllegalStateException("存在子目录，不能删除");
        }
        if (portfolioRepository.countActiveRecords(userId, catalogId) > 0) {
            throw new IllegalStateException("存在活跃资产记录，不能删除");
        }
        catalog.markDeleted();
        portfolioRepository.updateCatalog(catalog);
    }

    @Override
    public List<AssetRecordEntity> queryRecords(Long userId) {
        requireUserId(userId);
        return portfolioRepository.queryActiveRecords(userId);
    }

    @Override
    @Transactional
    public AssetRecordEntity createRecord(AssetManagementCommand.CreateRecord command) {
        if (command == null) throw new IllegalArgumentException("资产创建参数不能为空");
        AssetCatalogEntity catalog = requireRecordableCatalog(command.getUserId(), command.getCatalogId());
        RecordIdentity identity = resolveRecordIdentity(catalog, command.getAssetRef(), command.getRecordName());
        AssetRecordEntity record = AssetRecordEntity.create(command.getUserId(), catalog.getId(), identity.name,
                identity.kind, identity.assetId, command.getAmount(), command.getCurrency(), command.getRemark());
        record.attachCatalogContext(catalog.getCatalogCode(), catalog.getBalanceDirection());
        record.attachAssetRef(identity.assetRef);
        portfolioRepository.insertRecord(record);
        portfolioRepository.insertRecordChanges(List.of(change(UUID.randomUUID().toString(), record,
                AssetRecordChangeEntity.CREATE, null, record.getAmount(), null, record.getStatus())));
        return record;
    }

    @Override
    @Transactional
    public AssetRecordEntity updateRecordDetails(AssetManagementCommand.UpdateDetails command) {
        if (command == null) throw new IllegalArgumentException("资产详情更新参数不能为空");
        AssetRecordEntity record = requireRecord(command.getUserId(), command.getRecordId(), false);
        record.updateDetails(command.getRecordName(), command.getRemark());
        portfolioRepository.updateRecord(record);
        return record;
    }

    @Override
    @Transactional
    public AssetRecordEntity updateRecordAmount(AssetManagementCommand.UpdateAmount command) {
        if (command == null) throw new IllegalArgumentException("资产金额更新参数不能为空");
        AssetRecordEntity record = requireRecord(command.getUserId(), command.getRecordId(), true);
        BigDecimal before = record.updateAmount(command.getAmount());
        portfolioRepository.updateRecord(record);
        portfolioRepository.insertRecordChanges(List.of(change(UUID.randomUUID().toString(), record,
                AssetRecordChangeEntity.UPDATE_AMOUNT, before, record.getAmount(), record.getStatus(), record.getStatus())));
        return record;
    }

    @Override
    @Transactional
    public AssetRecordEntity archiveRecord(Long userId, Long recordId) {
        return changeStatus(userId, recordId, AssetRecordChangeEntity.ARCHIVE);
    }

    @Override
    @Transactional
    public AssetRecordEntity restoreRecord(Long userId, Long recordId) {
        return changeStatus(userId, recordId, AssetRecordChangeEntity.RESTORE);
    }

    @Override
    @Transactional
    public AssetRecordEntity deleteRecord(Long userId, Long recordId) {
        return changeStatus(userId, recordId, AssetRecordChangeEntity.DELETE);
    }

    @Override
    @Transactional
    public AssetRecordEntity splitRecord(AssetManagementCommand.SplitRecord command) {
        if (command == null) throw new IllegalArgumentException("资产拆分参数不能为空");
        AssetRecordEntity source = requireRecord(command.getUserId(), command.getSourceRecordId(), true);
        if (!source.isSplittable()) throw new IllegalStateException("当前资产记录不可拆分");
        RecordIdentity targetIdentity = resolveConcreteIdentity(source.getAssetKind(), command.getAssetRef());
        BigDecimal beforeAmount = source.getAmount();
        String beforeStatus = source.getStatus();
        BigDecimal splitAmount = source.splitOut(command.getAmount());
        AssetRecordEntity target = AssetRecordEntity.create(source.getUserId(), source.getCatalogId(),
                targetIdentity.name, source.getAssetKind(), targetIdentity.assetId, splitAmount,
                source.getCurrency(), command.getRemark());
        target.attachCatalogContext(source.getCatalogCode(), source.getBalanceDirection());
        target.attachAssetRef(targetIdentity.assetRef);

        portfolioRepository.insertRecord(target);
        portfolioRepository.updateRecord(source);
        String operationId = UUID.randomUUID().toString();
        portfolioRepository.insertRecordChanges(List.of(
                change(operationId, source, AssetRecordChangeEntity.SPLIT_OUT,
                        beforeAmount, source.getAmount(), beforeStatus, source.getStatus()),
                change(operationId, target, AssetRecordChangeEntity.SPLIT_IN,
                        null, target.getAmount(), null, target.getStatus())));
        return target;
    }

    @Override
    public AssetSummaryEntity summarize(Long userId, String targetCurrency) {
        requireUserId(userId);
        List<AssetRecordEntity> records = portfolioRepository.queryActiveRecords(userId);
        String target = ExchangeRateEntity.normalizeCurrency(targetCurrency == null ? ExchangeRateEntity.CNY : targetCurrency);
        Set<String> bases = new LinkedHashSet<>();
        for (AssetRecordEntity record : records) {
            if (!ExchangeRateEntity.CNY.equals(record.getCurrency())) bases.add(record.getCurrency());
        }
        if (!ExchangeRateEntity.CNY.equals(target)) bases.add(target);
        Map<String, ExchangeRateEntity> rates = new LinkedHashMap<>();
        for (ExchangeRateEntity rate : portfolioRepository.queryExchangeRates(bases, ExchangeRateEntity.CNY)) {
            rates.put(rate.getBaseCurrency(), rate);
        }
        return assetSummaryService.summarize(records, target, rates);
    }

    @Override
    @Transactional
    public ExchangeRateEntity upsertExchangeRate(AssetManagementCommand.UpsertExchangeRate command) {
        if (command == null) throw new IllegalArgumentException("汇率参数不能为空");
        ExchangeRateEntity rate = ExchangeRateEntity.current(command.getBaseCurrency(), command.getQuoteCurrency(),
                command.getRate(), command.getSource(), command.getSourceAsOf(),
                command.getFetchedAt() == null ? LocalDateTime.now() : command.getFetchedAt());
        portfolioRepository.upsertExchangeRate(rate);
        return portfolioRepository.queryExchangeRate(rate.getBaseCurrency(), rate.getQuoteCurrency());
    }

    @Override
    public List<ExchangeRateEntity> queryExchangeRates(List<String> baseCurrencies) {
        if (baseCurrencies == null || baseCurrencies.isEmpty()) return List.of();
        List<String> normalized = new ArrayList<>();
        for (String currency : baseCurrencies) {
            String value = ExchangeRateEntity.normalizeCurrency(currency);
            if (!ExchangeRateEntity.CNY.equals(value)) normalized.add(value);
        }
        return portfolioRepository.queryExchangeRates(normalized, ExchangeRateEntity.CNY);
    }

    private AssetRecordEntity changeStatus(Long userId, Long recordId, String type) {
        AssetRecordEntity record = requireRecord(userId, recordId, true);
        String before = record.getStatus();
        if (AssetRecordChangeEntity.ARCHIVE.equals(type)) record.archive();
        else if (AssetRecordChangeEntity.RESTORE.equals(type)) {
            requireRecordableCatalog(userId, record.getCatalogId());
            record.restore();
        }
        else if (AssetRecordChangeEntity.DELETE.equals(type)) record.delete();
        else throw new IllegalArgumentException("资产状态操作不支持");
        portfolioRepository.updateRecord(record);
        portfolioRepository.insertRecordChanges(List.of(change(UUID.randomUUID().toString(), record, type,
                record.getAmount(), record.getAmount(), before, record.getStatus())));
        return record;
    }

    private AssetRecordChangeEntity change(String operationId, AssetRecordEntity record, String type,
                                           BigDecimal beforeAmount, BigDecimal afterAmount,
                                           String beforeStatus, String afterStatus) {
        return AssetRecordChangeEntity.builder().operationId(operationId).userId(record.getUserId())
                .recordId(record.getId()).changeType(type).beforeAmount(beforeAmount).afterAmount(afterAmount)
                .currency(record.getCurrency()).beforeStatus(beforeStatus).afterStatus(afterStatus)
                .operatorId(record.getUserId()).build();
    }

    private AssetCatalogEntity requireRecordableCatalog(Long userId, Long catalogId) {
        AssetCatalogEntity catalog = requireCatalog(userId, catalogId);
        if (!catalog.isEnabled()) throw new IllegalStateException("资产目录未启用");
        if (portfolioRepository.countEnabledChildren(userId, catalogId) > 0) {
            throw new IllegalStateException("分组目录不能创建资产记录");
        }
        return catalog;
    }

    private AssetCatalogEntity requireCatalog(Long userId, Long catalogId) {
        requireUserId(userId);
        if (catalogId == null || catalogId <= 0) throw new IllegalArgumentException("目录ID不合法");
        AssetCatalogEntity catalog = portfolioRepository.queryVisibleCatalog(userId, catalogId);
        if (catalog == null) throw new IllegalArgumentException("资产目录不存在或不可见");
        return catalog;
    }

    private AssetRecordEntity requireRecord(Long userId, Long recordId, boolean forUpdate) {
        requireUserId(userId);
        if (recordId == null || recordId <= 0) throw new IllegalArgumentException("资产记录ID不合法");
        AssetRecordEntity record = forUpdate
                ? portfolioRepository.queryRecordForUpdate(userId, recordId)
                : portfolioRepository.queryRecord(userId, recordId);
        if (record == null) throw new IllegalArgumentException("资产记录不存在或不可见");
        return record;
    }

    private void validateParent(Long userId, Long parentId, String direction, Long movingCatalogId, boolean hasChildren) {
        AssetCatalogEntity.requireDirection(direction);
        if (parentId == null) return;
        AssetCatalogEntity parent = requireCatalog(userId, parentId);
        if (!parent.isEnabled()) throw new IllegalStateException("父目录未启用");
        if (parent.getParentId() != null) throw new IllegalStateException("资产目录最多两级");
        if (hasChildren) throw new IllegalStateException("有子目录的一级目录不能移动到其他目录下");
        if (!direction.equals(parent.getBalanceDirection())) throw new IllegalArgumentException("父子目录金额方向必须一致");
        if (parent.isSystem() && !AssetCatalogEntity.CODE_INVESTMENT_ASSET.equals(parent.getCatalogCode())) {
            throw new IllegalStateException("系统叶子目录不能增加用户子目录");
        }
        if (movingCatalogId != null && parent.getId().equals(movingCatalogId)) {
            throw new IllegalArgumentException("目录不能成为自己的子目录");
        }
    }

    private RecordIdentity resolveRecordIdentity(AssetCatalogEntity catalog, String assetRef, String requestedName) {
        if (catalog.isFundCatalog()) {
            if (isBlank(assetRef)) return new RecordIdentity(AssetRecordEntity.KIND_FUND, null, null,
                    isBlank(requestedName) ? "未细分基金" : requestedName.trim());
            return resolveConcreteIdentity(AssetRecordEntity.KIND_FUND, assetRef);
        }
        if (catalog.isStockCatalog()) {
            if (isBlank(assetRef)) return new RecordIdentity(AssetRecordEntity.KIND_STOCK, null, null,
                    isBlank(requestedName) ? "未细分股票" : requestedName.trim());
            return resolveConcreteIdentity(AssetRecordEntity.KIND_STOCK, assetRef);
        }
        if (!isBlank(assetRef)) throw new IllegalArgumentException("当前目录不允许选择公共基金或股票");
        return new RecordIdentity(null, null, null, requestedName);
    }

    private RecordIdentity resolveConcreteIdentity(String expectedKind, String assetRef) {
        String externalKind = expectedKind.toLowerCase(Locale.ROOT);
        MarketAssetRefVO ref = MarketAssetRefVO.parse(externalKind, assetRef);
        if (AssetRecordEntity.KIND_FUND.equals(expectedKind)) {
            FundCurrentDataAggregate.FundDetail fund = fundDataRepository.queryCurrentDetails(Set.of(ref.getAssetCode()))
                    .get(ref.getAssetCode());
            if (fund == null || fund.getId() == null || isBlank(fund.getFundName())) {
                throw new IllegalArgumentException("基金不存在或公开数据不完整");
            }
            return new RecordIdentity(expectedKind, fund.getId(), ref.value(), fund.getFundName().trim());
        }
        StockMarketEntity stock = stockMarketRepository.queryOne(ref.getAssetCode(), ref.getMarket());
        if (stock == null || stock.getId() == null || isBlank(stock.getStockName())
                || !StockMarketEntity.STATUS_ACTIVE.equals(stock.getStatus())) {
            throw new IllegalArgumentException("股票不存在、已停用或公开数据不完整");
        }
        return new RecordIdentity(expectedKind, stock.getId(), ref.value(), stock.getStockName().trim());
    }

    private static Long requireUserId(Long userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("用户ID不合法");
        return userId;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class RecordIdentity {
        private final String kind;
        private final Long assetId;
        private final String assetRef;
        private final String name;

        private RecordIdentity(String kind, Long assetId, String assetRef, String name) {
            this.kind = kind;
            this.assetId = assetId;
            this.assetRef = assetRef;
            this.name = name;
        }
    }
}
