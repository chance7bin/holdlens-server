package com.echoamoy.holdlens.server.cases.marketdetail;

import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailCommand;
import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailResult;

public interface IMarketDetailCase {
    MarketDetailResult.Task createAndDispatch(MarketDetailCommand.CreateTask command);
    MarketDetailResult.Task handleCallback(MarketDetailCommand.Callback command);
    MarketDetailResult.FundNavHistory queryFundNavHistory(String fundCode, String period);
    MarketDetailResult.StockPriceHistory queryStockPriceHistory(String assetRef, String period);
    MarketDetailResult.StockCompanyProfile queryStockCompanyProfile(String assetRef);
}
