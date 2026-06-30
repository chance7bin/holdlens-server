package com.echoamoy.holdlens.server.cases.portfolio.impl;

import com.echoamoy.holdlens.server.cases.portfolio.IWatchlistAssetBatchAddCase;
import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddCommand;
import com.echoamoy.holdlens.server.cases.portfolio.model.WatchlistAssetBatchAddResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
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
    private static final String STATUS_ENABLED = "enabled";

    @Resource
    private IPortfolioRepository portfolioRepository;

    @Resource
    private IFundDataRepository fundDataRepository;

    @Resource
    private IStockMarketRepository stockMarketRepository;

    @Override
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
                plan.getWatchlistAssets().add(toWatchlistAsset(item, fundDetail.getFundName()));
                continue;
            }
            if (ASSET_KIND_STOCK.equals(item.getAssetKind())) {
                StockMarketEntity stockMarket = stockMarkets.get(stockKey(item.getAssetCode(), item.getMarket()));
                if (stockMarket == null) {
                    plan.getInvalidItems().add(toInvalidItem(item, "STOCK_NOT_FOUND", "股票不存在"));
                    continue;
                }
                plan.getWatchlistAssets().add(toWatchlistAsset(item, stockMarket.getStockName()));
                continue;
            }
        }
        plan.getInvalidItems().sort(Comparator.comparing(WatchlistAssetBatchAddResult.InvalidItem::getIndex));
        return plan;
    }

    private NormalizedItem normalize(Long userId, int index, WatchlistAssetBatchAddCommand.Item item) {
        if (item == null) {
            return invalid(index, null, null, null, "ITEM_REQUIRED", "自选资产项不能为空");
        }
        String assetKind = normalizeKind(item.getAssetKind());
        String assetCode = normalizeNullable(item.getAssetCode());
        String assetName = normalizeNullable(item.getAssetName());
        String market = normalizeNullable(item.getMarket());

        if (isBlank(assetKind)) {
            return invalid(index, item.getAssetKind(), item.getAssetCode(), item.getMarket(),
                    "ASSET_KIND_REQUIRED", "资产类型不能为空");
        }
        if (!ASSET_KIND_FUND.equals(assetKind) && !ASSET_KIND_STOCK.equals(assetKind)) {
            return invalid(index, item.getAssetKind(), item.getAssetCode(), item.getMarket(),
                    "ASSET_KIND_UNSUPPORTED", "仅支持基金或股票资产");
        }
        if (isBlank(assetCode)) {
            return invalid(index, item.getAssetKind(), item.getAssetCode(), item.getMarket(),
                    "ASSET_CODE_REQUIRED", "资产代码不能为空");
        }

        return NormalizedItem.builder()
                .index(index)
                .userId(userId)
                .originalAssetKind(item.getAssetKind())
                .originalAssetCode(item.getAssetCode())
                .originalMarket(item.getMarket())
                .assetKind(assetKind)
                .assetCode(assetCode)
                .assetName(assetName)
                .market(market)
                .identityKey(userId + "#" + assetKind + "#" + assetCode)
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

    private WatchlistAssetEntity toWatchlistAsset(NormalizedItem item, String publicAssetName) {
        return WatchlistAssetEntity.builder()
                .userId(item.getUserId())
                .assetCode(item.getAssetCode())
                .assetName(firstNonBlank(publicAssetName, item.getAssetName()))
                .assetKind(item.getAssetKind())
                .market(item.getMarket())
                .status(STATUS_ENABLED)
                .build();
    }

    private WatchlistAssetBatchAddResult.InvalidItem toInvalidItem(NormalizedItem item, String reasonCode, String reason) {
        return WatchlistAssetBatchAddResult.InvalidItem.builder()
                .index(item.getIndex())
                .assetKind(item.getOriginalAssetKind())
                .assetCode(item.getOriginalAssetCode())
                .market(item.getOriginalMarket())
                .reasonCode(reasonCode)
                .reason(reason)
                .build();
    }

    private NormalizedItem invalid(int index, String assetKind, String assetCode, String market,
                                   String reasonCode, String reason) {
        return NormalizedItem.builder()
                .invalidItem(WatchlistAssetBatchAddResult.InvalidItem.builder()
                        .index(index)
                        .assetKind(assetKind)
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
