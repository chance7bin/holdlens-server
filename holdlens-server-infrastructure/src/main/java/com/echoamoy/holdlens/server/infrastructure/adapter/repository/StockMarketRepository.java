package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteEntity;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteTargetEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IStockMarketCurrentDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockMarketCurrentPO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class StockMarketRepository implements IStockMarketRepository {

    @Resource
    private IStockMarketCurrentDao stockMarketCurrentDao;

    @Override
    public List<StockQuoteTargetEntity> queryAllQuoteTargets() {
        return stockMarketCurrentDao.selectAllTargets().stream()
                .map(po -> StockQuoteTargetEntity.builder()
                        .id(po.getId())
                        .stockCode(po.getStockCode())
                        .market(po.getMarket())
                        .build())
                .toList();
    }

    @Override
    public List<StockQuoteTargetEntity> queryRefreshTargetsAfterId(Long lastId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return stockMarketCurrentDao.selectRefreshTargetsAfterId(lastId == null ? 0L : lastId, limit).stream()
                .map(po -> StockQuoteTargetEntity.builder()
                        .id(po.getId())
                        .stockCode(po.getStockCode())
                        .market(po.getMarket())
                        .build())
                .toList();
    }

    @Override
    public void registerQuoteTargets(List<StockQuoteEntity> quoteTargets) {
        if (quoteTargets == null || quoteTargets.isEmpty()) {
            return;
        }
        for (StockQuoteEntity quoteTarget : quoteTargets) {
            stockMarketCurrentDao.upsertTarget(toTargetPO(quoteTarget));
        }
    }

    @Override
    public void upsertQuotes(List<StockQuoteEntity> quotes) {
        if (quotes == null || quotes.isEmpty()) {
            return;
        }
        for (StockQuoteEntity quote : quotes) {
            stockMarketCurrentDao.upsert(toPO(quote));
        }
    }

    @Override
    public Map<String, StockQuoteEntity> queryByStockKeys(Collection<String> stockKeys) {
        if (stockKeys == null || stockKeys.isEmpty()) {
            return Map.of();
        }
        List<StockMarketCurrentPO> poList = stockMarketCurrentDao.selectByStockKeys(stockKeys);
        Map<String, StockQuoteEntity> result = new LinkedHashMap<>();
        for (StockMarketCurrentPO po : poList) {
            result.put(stockKey(po.getStockCode(), po.getMarket()), toEntity(po));
        }
        return result;
    }

    private StockMarketCurrentPO toPO(StockQuoteEntity quote) {
        return StockMarketCurrentPO.builder()
                .stockCode(quote.getStockCode())
                .market(quote.getMarket())
                .stockName(quote.getStockName())
                .tradeDate(quote.getTradeDate())
                .dailyReturn(quote.getDailyReturn())
                .quoteTime(quote.getQuoteTime())
                .build();
    }

    private StockMarketCurrentPO toTargetPO(StockQuoteEntity quoteTarget) {
        return StockMarketCurrentPO.builder()
                .stockCode(quoteTarget.getStockCode())
                .market(quoteTarget.getMarket())
                .stockName(quoteTarget.getStockName())
                .build();
    }

    private StockQuoteEntity toEntity(StockMarketCurrentPO po) {
        return StockQuoteEntity.builder()
                .stockCode(po.getStockCode())
                .market(po.getMarket())
                .stockName(po.getStockName())
                .tradeDate(po.getTradeDate())
                .dailyReturn(po.getDailyReturn())
                .quoteTime(po.getQuoteTime())
                .build();
    }

    private String stockKey(String stockCode, String market) {
        return stockCode + "#" + (market == null ? "" : market);
    }

}
