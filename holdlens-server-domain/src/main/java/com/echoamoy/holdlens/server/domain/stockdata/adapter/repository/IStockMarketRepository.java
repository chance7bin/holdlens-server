package com.echoamoy.holdlens.server.domain.stockdata.adapter.repository;

import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface IStockMarketRepository {

    void registerQuoteTargets(List<StockMarketEntity> quoteTargets);

    void upsertMarkets(List<StockMarketEntity> markets);

    Map<String, StockMarketEntity> queryByStockKeys(Collection<String> stockKeys);

    Set<String> queryExistingStockKeys(Collection<String> stockKeys);

}
