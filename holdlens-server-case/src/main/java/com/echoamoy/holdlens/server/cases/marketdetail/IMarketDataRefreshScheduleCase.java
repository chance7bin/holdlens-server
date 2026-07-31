package com.echoamoy.holdlens.server.cases.marketdetail;

public interface IMarketDataRefreshScheduleCase {
    boolean runAShareMarketRefresh();
    boolean runUSStockMarketRefresh();
    int runFundDetailRefresh();
    int runStockDetailRefresh(String market);
}
