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

    default List<StockMarketEntity> search(String keyword, String market, int limit) { throw unsupported("search"); }

    default StockMarketEntity queryOne(String stockCode, String market) { throw unsupported("queryOne"); }

    default void markDetailViewed(String stockCode, String market, LocalDateTime viewedAt,
                                  LocalDateTime updateBefore) { throw unsupported("markDetailViewed"); }

    default List<StockMarketEntity> queryDetailRefreshTargets(String market, LocalDateTime viewedSince,
                                                               int limit) { throw unsupported("queryDetailRefreshTargets"); }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException("IStockMarketRepository must implement " + operation);
    }

}
