package com.echoamoy.holdlens.server.cases.marketdetail;

import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailCommand;
import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailResult;

public interface IMarketDetailCase {
    MarketDetailResult.Task createAndDispatch(MarketDetailCommand.CreateTask command);
    MarketDetailResult.Task handleCallback(MarketDetailCommand.Callback command);
    MarketDetailResult.FundNavHistory queryFundNavHistory(String fundCode, String period);
    MarketDetailResult.FundPeriodPerformance queryFundPeriodPerformance(String fundCode);
    MarketDetailResult.FundDetailRefresh requestFundDetailRefresh(String fundCode);
    MarketDetailResult.StockPriceHistory queryStockPriceHistory(String assetRef, String period);
    MarketDetailResult.StockCompanyProfile queryStockCompanyProfile(String assetRef);
    MarketDetailResult.StockDetailRefresh ensureStockDetailData(String assetRef);
    MarketDetailResult.StockDetailRefresh queryStockDetailDataTask(String serverTaskId);
    MarketDetailResult.DetailRefresh ensureFundDetailData(String fundCode, boolean recordView);
    MarketDetailResult.DetailRefresh ensureStockDetailDataV2(String assetRef, boolean recordView);
    MarketDetailResult.DetailRefresh queryDetailOperation(String operationId);
    int scheduleActiveFundDetails();
    int scheduleActiveStockDetails(String market);
}
