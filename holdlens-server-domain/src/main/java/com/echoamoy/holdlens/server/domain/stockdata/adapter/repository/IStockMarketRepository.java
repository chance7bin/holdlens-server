package com.echoamoy.holdlens.server.domain.stockdata.adapter.repository;

import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

public interface IStockMarketRepository {

    void registerQuoteTargets(List<StockMarketEntity> quoteTargets);

    void upsertMarkets(List<StockMarketEntity> markets);

    Map<String, StockMarketEntity> queryByStockKeys(Collection<String> stockKeys);

    Set<String> queryExistingStockKeys(Collection<String> stockKeys);

    default List<StockMarketEntity> search(String keyword, String market, int limit) { return List.of(); }

    default StockMarketEntity queryOne(String stockCode, String market) { return null; }

    default void markDetailViewed(String stockCode, String market, LocalDateTime viewedAt,
                                  LocalDateTime updateBefore) { }

    default List<StockMarketEntity> queryDetailRefreshTargets(String market, LocalDateTime viewedSince,
                                                               int limit) { return List.of(); }

}
