package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundNavHistoryPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockCompanyProfilePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockPriceBarPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface IMarketDetailDao {
    void upsertFundNavHistory(@Param("points") List<FundNavHistoryPO> points);
    void upsertStockPriceBars(@Param("bars") List<StockPriceBarPO> bars);
    void upsertStockCompanyProfile(StockCompanyProfilePO profile);
    List<FundNavHistoryPO> selectFundNavHistory(@Param("fundCode") String fundCode,
                                                @Param("startDate") Date startDate);
    Date selectLatestFundNavDate(@Param("fundCode") String fundCode);
    List<StockPriceBarPO> selectStockPriceBars(@Param("stockCode") String stockCode,
                                               @Param("market") String market,
                                               @Param("granularity") String granularity,
                                               @Param("startTime") Date startTime);
    Date selectLatestStockBarTime(@Param("stockCode") String stockCode, @Param("market") String market,
                                  @Param("granularity") String granularity);
    StockCompanyProfilePO selectStockCompanyProfile(@Param("stockCode") String stockCode,
                                                     @Param("market") String market);
}
