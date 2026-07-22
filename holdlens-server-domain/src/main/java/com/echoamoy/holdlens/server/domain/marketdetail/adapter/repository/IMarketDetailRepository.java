package com.echoamoy.holdlens.server.domain.marketdetail.adapter.repository;

import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.FundNavHistoryEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.StockCompanyProfileEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.StockPriceBarEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IMarketDetailRepository {
    void upsertFundNavHistory(List<FundNavHistoryEntity> points);
    void upsertStockPriceBars(List<StockPriceBarEntity> bars);
    void upsertStockCompanyProfile(StockCompanyProfileEntity profile);
    List<FundNavHistoryEntity> queryFundNavHistory(String fundCode, LocalDate startDate);
    LocalDate queryLatestFundNavDate(String fundCode);
    List<StockPriceBarEntity> queryStockPriceBars(String stockCode, String market, String granularity,
                                                  LocalDateTime startTime);
    LocalDateTime queryLatestStockBarTime(String stockCode, String market, String granularity);
    StockCompanyProfileEntity queryStockCompanyProfile(String stockCode, String market);
}
