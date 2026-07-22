package com.echoamoy.holdlens.server.cases.marketasset.impl;

import com.echoamoy.holdlens.server.cases.marketasset.IMarketAssetQueryCase;
import com.echoamoy.holdlens.server.cases.marketasset.model.MarketAssetQueryResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.marketasset.model.valobj.MarketAssetRefVO;
import com.echoamoy.holdlens.server.domain.portfolio.adapter.repository.IPortfolioRepository;
import com.echoamoy.holdlens.server.domain.portfolio.model.entity.WatchlistAssetEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MarketAssetQueryCaseImpl implements IMarketAssetQueryCase {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource private IPortfolioRepository portfolioRepository;
    @Resource private IFundDataRepository fundDataRepository;
    @Resource private IStockMarketRepository stockMarketRepository;

    @Override
    public MarketAssetQueryResult.Watchlist queryWatchlist(Long userId, String assetKind) {
        requireUser(userId);
        String kind = normalizeKindFilter(assetKind);
        List<WatchlistAssetEntity> relations = portfolioRepository.queryWatchlistAssets(userId, kind);
        List<WatchlistAssetEntity> countRelations = kind == null
                ? relations : portfolioRepository.queryWatchlistAssets(userId, null);
        Set<String> fundCodes = new LinkedHashSet<>();
        Set<String> stockKeys = new LinkedHashSet<>();
        for (WatchlistAssetEntity relation : relations) {
            if (MarketAssetRefVO.KIND_FUND.equals(relation.getAssetKind())) {
                fundCodes.add(relation.getAssetCode());
            } else if (MarketAssetRefVO.KIND_STOCK.equals(relation.getAssetKind())) {
                stockKeys.add(stockKey(relation.getAssetCode(), relation.getMarket()));
            }
        }
        Map<String, FundCurrentDataAggregate.FundDetail> funds = fundDataRepository.queryCurrentDetails(fundCodes);
        Map<String, StockMarketEntity> stocks = stockMarketRepository.queryByStockKeys(stockKeys);
        List<MarketAssetQueryResult.Item> items = new ArrayList<>();
        for (WatchlistAssetEntity relation : relations) {
            if (MarketAssetRefVO.KIND_FUND.equals(relation.getAssetKind())) {
                items.add(toFundItem(relation, funds.get(relation.getAssetCode()), null));
            } else if (MarketAssetRefVO.KIND_STOCK.equals(relation.getAssetKind())) {
                items.add(toStockItem(relation, stocks.get(stockKey(relation.getAssetCode(), relation.getMarket())), null));
            }
        }
        int fundCount = (int) countRelations.stream()
                .filter(relation -> MarketAssetRefVO.KIND_FUND.equals(relation.getAssetKind())).count();
        int stockCount = (int) countRelations.stream()
                .filter(relation -> MarketAssetRefVO.KIND_STOCK.equals(relation.getAssetKind())).count();
        return MarketAssetQueryResult.Watchlist.builder()
                .fundCount(fundCount).stockCount(stockCount).items(items).build();
    }

    @Override
    public MarketAssetQueryResult.Search search(Long userId, String keyword, String assetKind, String market, Integer limit) {
        requireUser(userId);
        if (keyword == null || keyword.trim().isEmpty()) {
            throw illegal("搜索关键字不能为空");
        }
        if (keyword.trim().length() > 100) {
            throw illegal("搜索关键字过长");
        }
        int safeLimit = limit == null ? 20 : limit;
        if (safeLimit < 1 || safeLimit > 50) {
            throw illegal("limit 必须在 1-50 之间");
        }
        String kind = normalizeSearchKind(assetKind);
        String marketFilter = normalizeMarketFilter(market);
        List<MarketAssetQueryResult.Item> candidates = new ArrayList<>();
        String q = keyword.trim();
        if (!MarketAssetRefVO.KIND_STOCK.equals(kind)) {
            for (FundCurrentDataAggregate.FundDetail fund : fundDataRepository.search(q, safeLimit)) {
                candidates.add(toFundItem(null, fund, false));
            }
        }
        if (!MarketAssetRefVO.KIND_FUND.equals(kind) && candidates.size() < safeLimit) {
            int stockLimit = safeLimit - candidates.size();
            for (StockMarketEntity stock : stockMarketRepository.search(q, marketFilter, stockLimit)) {
                candidates.add(toStockItem(null, stock, false));
            }
        }
        Set<String> identities = new LinkedHashSet<>();
        for (MarketAssetQueryResult.Item item : candidates) {
            identities.add(item.getAssetKind() + "#" + item.getCode());
        }
        Set<String> watchlisted = portfolioRepository.queryWatchlistedIdentityKeys(userId, identities);
        for (MarketAssetQueryResult.Item item : candidates) {
            item.setWatchlisted(watchlisted.contains(item.getAssetKind() + "#" + item.getCode()));
        }
        return MarketAssetQueryResult.Search.builder().items(candidates).build();
    }

    @Override
    public MarketAssetQueryResult.StockDetail queryStockDetail(Long userId, String assetRef) {
        requireUser(userId);
        MarketAssetRefVO ref;
        try {
            ref = MarketAssetRefVO.parse(MarketAssetRefVO.KIND_STOCK, assetRef);
        } catch (IllegalArgumentException e) {
            throw illegal(e.getMessage());
        }
        StockMarketEntity stock = stockMarketRepository.queryOne(ref.getAssetCode(), ref.getMarket());
        if (stock == null) {
            throw illegal("股票不存在");
        }
        boolean watchlisted = portfolioRepository.queryWatchlistAsset(userId, ref.getAssetCode(), ref.getAssetKind()) != null;
        return MarketAssetQueryResult.StockDetail.builder()
                .assetKind(ref.getAssetKind()).assetRef(ref.value()).code(stock.getStockCode())
                .name(stock.getStockName()).market(stock.getMarket()).marketLabel(marketLabel(stock.getMarket()))
                .currency(stock.getCurrency()).latestPrice(stock.getLatestPrice()).changeAmount(stock.getChangeAmount())
                .changePercent(stock.getChangePercent()).openPrice(stock.getOpenPrice()).highPrice(stock.getHighPrice())
                .lowPrice(stock.getLowPrice()).previousClose(stock.getPreviousClose()).volume(stock.getVolume())
                .volumeUnit(stock.getVolumeUnit()).peRatio(stock.getPeRatio()).totalMarketValue(stock.getTotalMarketValue())
                .quoteAsOf(format(stock.getRefreshedAt())).delayNotice("行情可能延迟").watchlisted(watchlisted).build();
    }

    private MarketAssetQueryResult.Item toFundItem(WatchlistAssetEntity relation,
                                                    FundCurrentDataAggregate.FundDetail fund, Boolean watchlisted) {
        String code = fund == null ? relation.getAssetCode() : fund.getFundCode();
        return MarketAssetQueryResult.Item.builder()
                .assetKind(MarketAssetRefVO.KIND_FUND).assetRef(MarketAssetRefVO.fund(code).value()).code(code)
                .name(fund == null ? relation.getAssetName() : fund.getFundName())
                .assetType(fund == null ? relation.getAssetType() : fund.getFundType())
                .currency("CNY").latestValue(fund == null ? null : fund.getUnitNav())
                .changePercent(fund == null ? null : fund.getDailyGrowthRate())
                .valueAsOf(fund == null || fund.getReturnsAsOf() == null ? null
                        : new java.sql.Date(fund.getReturnsAsOf().getTime()).toLocalDate().toString())
                .watchlisted(watchlisted).build();
    }

    private MarketAssetQueryResult.Item toStockItem(WatchlistAssetEntity relation,
                                                     StockMarketEntity stock, Boolean watchlisted) {
        String code = stock == null ? relation.getAssetCode() : stock.getStockCode();
        String market = stock == null ? relation.getMarket() : stock.getMarket();
        return MarketAssetQueryResult.Item.builder()
                .assetKind(MarketAssetRefVO.KIND_STOCK).assetRef(stockAssetRefOrNull(market, code)).code(code)
                .name(stock == null ? relation.getAssetName() : stock.getStockName())
                .assetType(relation == null ? "普通股票" : relation.getAssetType()).market(market)
                .marketLabel(marketLabel(market)).currency(stock == null ? null : stock.getCurrency())
                .latestValue(stock == null ? null : stock.getLatestPrice())
                .changePercent(stock == null ? null : stock.getChangePercent())
                .valueAsOf(stock == null ? null : format(stock.getRefreshedAt())).watchlisted(watchlisted).build();
    }

    private String normalizeKindFilter(String kind) {
        if (kind == null || kind.isBlank()) return null;
        String value = kind.trim().toLowerCase(Locale.ROOT);
        if (!MarketAssetRefVO.KIND_FUND.equals(value) && !MarketAssetRefVO.KIND_STOCK.equals(value)) {
            throw illegal("assetKind 不支持");
        }
        return value;
    }

    private String normalizeSearchKind(String kind) {
        if (kind == null || kind.isBlank() || "all".equalsIgnoreCase(kind)) return null;
        return normalizeKindFilter(kind);
    }

    private String normalizeMarketFilter(String market) {
        if (market == null || market.isBlank() || "all".equalsIgnoreCase(market)) return null;
        String value = market.trim().toUpperCase(Locale.ROOT);
        if (!MarketAssetRefVO.MARKET_A_SHARE.equals(value) && !MarketAssetRefVO.MARKET_US_STOCK.equals(value)) {
            throw illegal("market 不支持");
        }
        return value;
    }

    private void requireUser(Long userId) {
        if (userId == null || userId <= 0) throw illegal("用户ID不合法");
    }

    private String format(java.time.LocalDateTime value) {
        if (value == null) return null;
        ZoneOffset offset = BUSINESS_ZONE.getRules().getOffset(value);
        return value.atOffset(offset).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String marketLabel(String market) {
        if (MarketAssetRefVO.MARKET_US_STOCK.equals(market)) return "美股";
        if (MarketAssetRefVO.MARKET_A_SHARE.equals(market)) return "A股";
        return null;
    }

    private String stockAssetRefOrNull(String market, String code) {
        if (!MarketAssetRefVO.MARKET_A_SHARE.equals(market)
                && !MarketAssetRefVO.MARKET_US_STOCK.equals(market)) {
            return null;
        }
        return MarketAssetRefVO.stock(market, code).value();
    }

    private String stockKey(String code, String market) {
        return code + "#" + (market == null ? "" : market);
    }

    private AppException illegal(String message) {
        return new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), message);
    }
}
