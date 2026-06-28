package com.echoamoy.holdlens.server.domain.stockdata.adapter.repository;

import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteEntity;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteTargetEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IStockMarketRepository {

    List<StockQuoteTargetEntity> queryAllQuoteTargets();

    List<StockQuoteTargetEntity> queryRefreshTargetsAfterId(Long lastId, int limit);

    void registerQuoteTargets(List<StockQuoteEntity> quoteTargets);

    void upsertQuotes(List<StockQuoteEntity> quotes);

    Map<String, StockQuoteEntity> queryByStockKeys(Collection<String> stockKeys);

}
