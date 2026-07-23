package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.IWatchlistAssetBatchAddCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddCommand;
import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.marketasset.model.valobj.MarketAssetRefVO;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class WatchlistAssetBatchAddCaseImpl implements IWatchlistAssetBatchAddCase {

    private static final String ASSET_KIND_FUND = "fund";
    private static final String ASSET_KIND_STOCK = "stock";

    @Resource
    private IPortfolioRepository portfolioRepository;

    @Resource
    private IFundDataRepository fundDataRepository;

    @Resource
    private IStockMarketRepository stockMarketRepository;

    @Override
    @Transactional
    public WatchlistAssetBatchAddResult batchAdd(WatchlistAssetBatchAddCommand command) {
        if (command == null || command.getUserId() == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户ID不能为空");
        }

        BatchAddPlan plan = buildBatchAddPlan(command);
        portfolioRepository.upsertWatchlistAssets(plan.getWatchlistAssets());

        return WatchlistAssetBatchAddResult.builder()
                .invalidItems(plan.getInvalidItems())
                .build();
    }

    @Override
    @Transactional
    public void remove(Long userId, String assetKind, String assetRef) {
        if (userId == null || userId <= 0) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户ID不合法");
        }
        MarketAssetRefVO ref = MarketAssetRefVO.parse(assetKind, assetRef);
        Long assetId;
        if (ASSET_KIND_FUND.equals(ref.getAssetKind())) {
            FundCurrentDataAggregate.FundDetail fund = fundDataRepository.queryCurrentDetails(Set.of(ref.getAssetCode()))
                    .get(ref.getAssetCode());
            if (fund == null || fund.getId() == null) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "基金不存在");
            }
            assetId = fund.getId();
        } else {
            StockMarketEntity stock = stockMarketRepository.queryOne(ref.getAssetCode(), ref.getMarket());
            if (stock == null || stock.getId() == null) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "股票不存在");
            }
            assetId = stock.getId();
        }
        portfolioRepository.deleteWatchlistAsset(userId, ref.getAssetKind(), assetId);
    }

    private BatchAddPlan buildBatchAddPlan(WatchlistAssetBatchAddCommand command) {
        BatchAddPlan plan = BatchAddPlan.builder()
                .watchlistAssets(new ArrayList<>())
                .invalidItems(new ArrayList<>())
                .build();
        if (command.getItems() == null || command.getItems().isEmpty()) {
            return plan;
        }

        Map<String, NormalizedItem> dedup = new LinkedHashMap<>();
        List<WatchlistAssetBatchAddCommand.Item> items = command.getItems();
        for (int index = 0; index < items.size(); index++) {
            WatchlistAssetBatchAddCommand.Item item = items.get(index);
            NormalizedItem normalized = normalize(command.getUserId(), index, item);
            if (normalized.getInvalidItem() != null) {
                plan.getInvalidItems().add(normalized.getInvalidItem());
                continue;
            }
            dedup.putIfAbsent(normalized.getIdentityKey(), normalized);
        }

        Map<String, FundCurrentDataAggregate.FundDetail> fundDetails = fundDataRepository.queryCurrentDetails(collectFundCodes(dedup.values()));
        Map<String, StockMarketEntity> stockMarkets = stockMarketRepository.queryByStockKeys(collectStockKeys(dedup.values()));

        for (NormalizedItem item : dedup.values()) {
            if (ASSET_KIND_FUND.equals(item.getAssetKind())) {
                FundCurrentDataAggregate.FundDetail fundDetail = fundDetails.get(item.getAssetCode());
                if (fundDetail == null) {
                    plan.getInvalidItems().add(toInvalidItem(item, "FUND_NOT_FOUND", "基金不存在"));
                    continue;
                }
                plan.getWatchlistAssets().add(toWatchlistAsset(item, fundDetail.getId(), fundDetail.getFundName()));
                continue;
            }
            if (ASSET_KIND_STOCK.equals(item.getAssetKind())) {
                StockMarketEntity stockMarket = stockMarkets.get(stockKey(item.getAssetCode(), item.getMarket()));
                if (stockMarket == null) {
                    plan.getInvalidItems().add(toInvalidItem(item, "STOCK_NOT_FOUND", "股票不存在"));
                    continue;
                }
                if (!StockMarketEntity.STATUS_ACTIVE.equals(stockMarket.getStatus())) {
                    plan.getInvalidItems().add(toInvalidItem(item, "STOCK_INACTIVE", "股票已停用"));
                    continue;
                }
                if (isBlank(stockMarket.getStockName())) {
                    plan.getInvalidItems().add(toInvalidItem(item, "ASSET_NAME_MISSING", "股票公开名称缺失"));
                    continue;
                }
                plan.getWatchlistAssets().add(toWatchlistAsset(item, stockMarket.getId(), stockMarket.getStockName()));
                continue;
            }
        }
        plan.getInvalidItems().sort(Comparator.comparing(WatchlistAssetBatchAddResult.InvalidItem::getIndex));
        return plan;
    }

    private NormalizedItem normalize(Long userId, int index, WatchlistAssetBatchAddCommand.Item item) {
        if (item == null) {
            return invalid(index, null, null, null, null, "ITEM_REQUIRED", "自选资产项不能为空");
        }
        String assetKind = normalizeKind(item.getAssetKind());
        String assetCode = normalizeNullable(item.getAssetCode());
        String assetName = normalizeNullable(item.getAssetName());
        String market = normalizeNullable(item.getMarket());

        if (!isBlank(item.getAssetRef())) {
            try {
                MarketAssetRefVO ref = MarketAssetRefVO.parse(assetKind, item.getAssetRef());
                if (assetCode != null && !assetCode.equals(ref.getAssetCode())) {
                    return invalid(index, item.getAssetKind(), item.getAssetRef(), item.getAssetCode(), item.getMarket(),
                            "ASSET_REF_CONFLICT", "assetRef 与兼容资产代码冲突");
                }
                if (market != null && ref.getMarket() != null && !market.equalsIgnoreCase(ref.getMarket())) {
                    return invalid(index, item.getAssetKind(), item.getAssetRef(), item.getAssetCode(), item.getMarket(),
                            "ASSET_REF_CONFLICT", "assetRef 与兼容市场冲突");
                }
                assetKind = ref.getAssetKind();
                assetCode = ref.getAssetCode();
                market = ref.getMarket();
            } catch (IllegalArgumentException exception) {
                return invalid(index, item.getAssetKind(), item.getAssetRef(), item.getAssetCode(), item.getMarket(),
                        "ASSET_REF_INVALID", exception.getMessage());
            }
        }

        if (isBlank(assetKind)) {
            return invalid(index, item.getAssetKind(), item.getAssetRef(), item.getAssetCode(), item.getMarket(),
                    "ASSET_KIND_REQUIRED", "资产类型不能为空");
        }
        if (!ASSET_KIND_FUND.equals(assetKind) && !ASSET_KIND_STOCK.equals(assetKind)) {
            return invalid(index, item.getAssetKind(), item.getAssetRef(), item.getAssetCode(), item.getMarket(),
                    "ASSET_KIND_UNSUPPORTED", "仅支持基金或股票资产");
        }
        if (isBlank(assetCode)) {
            return invalid(index, item.getAssetKind(), item.getAssetRef(), item.getAssetCode(), item.getMarket(),
                    "ASSET_CODE_REQUIRED", "资产代码不能为空");
        }
        if (ASSET_KIND_STOCK.equals(assetKind) && isBlank(market)) {
            return invalid(index, item.getAssetKind(), item.getAssetRef(), item.getAssetCode(), item.getMarket(),
                    "MARKET_REQUIRED", "股票市场不能为空");
        }
        if (ASSET_KIND_STOCK.equals(assetKind)
                && !MarketAssetRefVO.MARKET_A_SHARE.equalsIgnoreCase(market)
                && !MarketAssetRefVO.MARKET_US_STOCK.equalsIgnoreCase(market)) {
            return invalid(index, item.getAssetKind(), item.getAssetRef(), item.getAssetCode(), item.getMarket(),
                    "MARKET_UNSUPPORTED", "股票市场不支持");
        }
        if (ASSET_KIND_STOCK.equals(assetKind)) {
            market = market.toUpperCase(Locale.ROOT);
        }

        return NormalizedItem.builder()
                .index(index)
                .userId(userId)
                .originalAssetKind(item.getAssetKind())
                .originalAssetRef(item.getAssetRef())
                .originalAssetCode(item.getAssetCode())
                .originalMarket(item.getMarket())
                .assetKind(assetKind)
                .assetCode(assetCode)
                .assetName(assetName)
                .market(market)
                .identityKey(userId + "#" + assetKind + "#" + assetCode
                        + (ASSET_KIND_STOCK.equals(assetKind) ? "#" + market : ""))
                .build();
    }

    private Set<String> collectFundCodes(Iterable<NormalizedItem> items) {
        Set<String> result = new LinkedHashSet<>();
        for (NormalizedItem item : items) {
            if (ASSET_KIND_FUND.equals(item.getAssetKind())) {
                result.add(item.getAssetCode());
            }
        }
        return result;
    }

    private Set<String> collectStockKeys(Iterable<NormalizedItem> items) {
        Set<String> result = new LinkedHashSet<>();
        for (NormalizedItem item : items) {
            if (ASSET_KIND_STOCK.equals(item.getAssetKind())) {
                result.add(stockKey(item.getAssetCode(), item.getMarket()));
            }
        }
        return result;
    }

    private WatchlistAssetEntity toWatchlistAsset(NormalizedItem item, Long assetId, String publicAssetName) {
        return WatchlistAssetEntity.builder()
                .userId(item.getUserId())
                .assetId(assetId)
                .assetCode(item.getAssetCode())
                .assetName(firstNonBlank(publicAssetName, item.getAssetName()))
                .assetKind(item.getAssetKind())
                .market(item.getMarket())
                .build();
    }

    private WatchlistAssetBatchAddResult.InvalidItem toInvalidItem(NormalizedItem item, String reasonCode, String reason) {
        return WatchlistAssetBatchAddResult.InvalidItem.builder()
                .index(item.getIndex())
                .assetKind(item.getOriginalAssetKind())
                .assetRef(item.getOriginalAssetRef())
                .assetCode(item.getOriginalAssetCode())
                .market(item.getOriginalMarket())
                .reasonCode(reasonCode)
                .reason(reason)
                .build();
    }

    private NormalizedItem invalid(int index, String assetKind, String assetRef, String assetCode, String market,
                                   String reasonCode, String reason) {
        return NormalizedItem.builder()
                .invalidItem(WatchlistAssetBatchAddResult.InvalidItem.builder()
                        .index(index)
                        .assetKind(assetKind)
                        .assetRef(assetRef)
                        .assetCode(assetCode)
                        .market(market)
                        .reasonCode(reasonCode)
                        .reason(reason)
                        .build())
                .build();
    }

    private String normalizeKind(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private String marketKey(String market) {
        return market == null ? "" : market;
    }

    private String stockKey(String stockCode, String market) {
        return stockCode + "#" + marketKey(market);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String firstNonBlank(String first, String second) {
        if (!isBlank(first)) {
            return first.trim();
        }
        return normalizeNullable(second);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class BatchAddPlan {

        private List<WatchlistAssetEntity> watchlistAssets;

        private List<WatchlistAssetBatchAddResult.InvalidItem> invalidItems;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class NormalizedItem {

        private Integer index;

        private Long userId;

        private String originalAssetKind;

        private String originalAssetRef;

        private String originalAssetCode;

        private String originalMarket;

        private String assetKind;

        private String assetCode;

        private String assetName;

        private String market;

        private String identityKey;

        private WatchlistAssetBatchAddResult.InvalidItem invalidItem;

    }

}
