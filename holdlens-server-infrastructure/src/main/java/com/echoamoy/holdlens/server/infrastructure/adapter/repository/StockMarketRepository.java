package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import com.echoamoy.holdlens.server.infrastructure.dao.IStockMarketDao;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockMarketPO;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class StockMarketRepository implements IStockMarketRepository {

    @Resource
    private IStockMarketDao stockMarketDao;

    @Override
    public void registerQuoteTargets(List<StockMarketEntity> quoteTargets) {
        if (quoteTargets == null || quoteTargets.isEmpty()) {
            return;
        }
        for (StockMarketEntity quoteTarget : quoteTargets) {
            stockMarketDao.upsertTarget(toTargetPO(quoteTarget));
        }
    }

    @Override
    public void upsertMarkets(List<StockMarketEntity> markets) {
        if (markets == null || markets.isEmpty()) {
            return;
        }
        for (StockMarketEntity market : markets) {
            stockMarketDao.upsert(toPO(market));
        }
    }

    @Override
    public Map<String, StockMarketEntity> queryByStockKeys(Collection<String> stockKeys) {
        if (stockKeys == null || stockKeys.isEmpty()) {
            return Map.of();
        }
        List<StockMarketPO> poList = stockMarketDao.selectByStockKeys(stockKeys);
        Map<String, StockMarketEntity> result = new LinkedHashMap<>();
        for (StockMarketPO po : poList) {
            result.put(stockKey(po.getStockCode(), po.getMarket()), toEntity(po));
        }
        return result;
    }

    @Override
    public Set<String> queryExistingStockKeys(Collection<String> stockKeys) {
        if (stockKeys == null || stockKeys.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (StockMarketPO po : stockMarketDao.selectByStockKeys(stockKeys)) {
            result.add(stockKey(po.getStockCode(), po.getMarket()));
        }
        return result;
    }

    private StockMarketPO toPO(StockMarketEntity market) {
        return StockMarketPO.builder()
                .id(market.getId())
                .stockCode(market.getStockCode())
                .market(market.getMarket())
                .exchangeCode(market.getExchangeCode())
                .providerMarketCode(market.getProviderMarketCode())
                .stockName(market.getStockName())
                .latestPrice(market.getLatestPrice())
                .changePercent(market.getChangePercent())
                .changeAmount(market.getChangeAmount())
                .volume(market.getVolume())
                .turnoverAmount(market.getTurnoverAmount())
                .amplitude(market.getAmplitude())
                .highPrice(market.getHighPrice())
                .lowPrice(market.getLowPrice())
                .openPrice(market.getOpenPrice())
                .previousClose(market.getPreviousClose())
                .volumeRatio(market.getVolumeRatio())
                .turnoverRate(market.getTurnoverRate())
                .peDynamic(market.getPeDynamic())
                .peRatio(market.getPeRatio())
                .pbRatio(market.getPbRatio())
                .totalMarketValue(market.getTotalMarketValue())
                .circulatingMarketValue(market.getCirculatingMarketValue())
                .speed(market.getSpeed())
                .fiveMinuteChange(market.getFiveMinuteChange())
                .sixtyDayChangePercent(market.getSixtyDayChangePercent())
                .yearToDateChangePercent(market.getYearToDateChangePercent())
                .listingDate(market.getListingDate())
                .status(market.getStatus())
                .refreshedAt(market.getRefreshedAt())
                .build();
    }

    private StockMarketPO toTargetPO(StockMarketEntity quoteTarget) {
        return StockMarketPO.builder()
                .stockCode(quoteTarget.getStockCode())
                .market(quoteTarget.getMarket())
                .exchangeCode(quoteTarget.getExchangeCode())
                .providerMarketCode(quoteTarget.getProviderMarketCode())
                .stockName(quoteTarget.getStockName())
                .status(StockMarketEntity.STATUS_ACTIVE)
                .build();
    }

    private StockMarketEntity toEntity(StockMarketPO po) {
        return StockMarketEntity.builder()
                .id(po.getId())
                .stockCode(po.getStockCode())
                .market(po.getMarket())
                .exchangeCode(po.getExchangeCode())
                .providerMarketCode(po.getProviderMarketCode())
                .stockName(po.getStockName())
                .latestPrice(po.getLatestPrice())
                .changePercent(po.getChangePercent())
                .changeAmount(po.getChangeAmount())
                .volume(po.getVolume())
                .turnoverAmount(po.getTurnoverAmount())
                .amplitude(po.getAmplitude())
                .highPrice(po.getHighPrice())
                .lowPrice(po.getLowPrice())
                .openPrice(po.getOpenPrice())
                .previousClose(po.getPreviousClose())
                .volumeRatio(po.getVolumeRatio())
                .turnoverRate(po.getTurnoverRate())
                .peDynamic(po.getPeDynamic())
                .peRatio(po.getPeRatio())
                .pbRatio(po.getPbRatio())
                .totalMarketValue(po.getTotalMarketValue())
                .circulatingMarketValue(po.getCirculatingMarketValue())
                .speed(po.getSpeed())
                .fiveMinuteChange(po.getFiveMinuteChange())
                .sixtyDayChangePercent(po.getSixtyDayChangePercent())
                .yearToDateChangePercent(po.getYearToDateChangePercent())
                .listingDate(po.getListingDate())
                .status(po.getStatus())
                .refreshedAt(po.getRefreshedAt())
                .build();
    }

    private String stockKey(String stockCode, String market) {
        return stockCode + "#" + (market == null ? "" : market);
    }

}
