package com.echoamoy.holdlens.server.infrastructure.dao;

import com.echoamoy.holdlens.server.infrastructure.dao.po.FundNavHistoryPO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.FundPeriodPerformancePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.MarketDetailSliceStatePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockCompanyProfilePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockDetailSliceStatePO;
import com.echoamoy.holdlens.server.infrastructure.dao.po.StockPriceBarPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface IMarketDetailDao {
    void upsertFundNavHistory(@Param("points") List<FundNavHistoryPO> points);
    void upsertFundPeriodPerformance(@Param("rows") List<FundPeriodPerformancePO> rows);
    void upsertStockPriceBars(@Param("bars") List<StockPriceBarPO> bars);
    void upsertStockCompanyProfile(StockCompanyProfilePO profile);
    List<FundNavHistoryPO> selectFundNavHistory(@Param("fundCode") String fundCode,
                                                @Param("startDate") Date startDate);
    Date selectLatestFundNavDate(@Param("fundCode") String fundCode);
    List<FundPeriodPerformancePO> selectFundPeriodPerformance(@Param("fundCode") String fundCode);
    void insertFundSliceStatesIfAbsent(@Param("fundCode") String fundCode,
                                       @Param("sliceTypes") List<String> sliceTypes);
    MarketDetailSliceStatePO selectFundSliceStateForUpdate(@Param("fundCode") String fundCode,
                                                            @Param("sliceType") String sliceType);
    int updateFundSliceState(MarketDetailSliceStatePO state);
    int updateFundSliceStateIfActiveTask(MarketDetailSliceStatePO state);
    List<StockPriceBarPO> selectStockPriceBars(@Param("stockCode") String stockCode,
                                               @Param("market") String market,
                                               @Param("granularity") String granularity,
                                               @Param("startTime") Date startTime);
    Date selectLatestStockBarTime(@Param("stockCode") String stockCode, @Param("market") String market,
                                  @Param("granularity") String granularity);
    StockCompanyProfilePO selectStockCompanyProfile(@Param("stockCode") String stockCode,
                                                     @Param("market") String market);
    void insertStockSliceStatesIfAbsent(@Param("assetRef") String assetRef,
                                        @Param("sliceTypes") List<String> sliceTypes);
    StockDetailSliceStatePO selectStockSliceStateForUpdate(@Param("assetRef") String assetRef,
                                                            @Param("sliceType") String sliceType);
    List<StockDetailSliceStatePO> selectStockSliceStates(@Param("assetRef") String assetRef);
    int updateStockSliceState(StockDetailSliceStatePO state);
    int updateStockSliceStateIfActiveTask(StockDetailSliceStatePO state);
}
