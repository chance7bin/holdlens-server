package com.echoamoy.holdlens.server.cases.agent;

import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCreateCommand;

public interface IAgentFundRefreshCase {

    FundRefreshTaskResult createAndDispatchAShareMarket(AShareMarketRefreshCreateCommand command);

    FundRefreshTaskResult handleAShareMarketCallback(AShareMarketRefreshCallbackCommand command);

    FundRefreshTaskResult createAndDispatchUSStockMarket(USStockMarketRefreshCreateCommand command);

    FundRefreshTaskResult handleUSStockMarketCallback(USStockMarketRefreshCallbackCommand command);

}
