package com.echoamoy.holdlens.server.cases.agent;

import com.echoamoy.holdlens.server.cases.agent.model.AgentFundRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AgentStockQuoteRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.StockQuoteRefreshCreateCommand;

public interface IAgentFundRefreshCase {

    FundRefreshTaskResult createAndDispatch(FundRefreshCreateCommand command);

    FundRefreshTaskResult handleCallback(AgentFundRefreshCallbackCommand command);

    FundRefreshTaskResult queryTask(String serverTaskId);

    FundRefreshTaskResult createAndDispatchStockQuotes();

    FundRefreshTaskResult createAndDispatchStockQuotes(StockQuoteRefreshCreateCommand command);

    boolean hasNonTerminalTask(String taskType);

    FundRefreshTaskResult handleStockQuoteCallback(AgentStockQuoteRefreshCallbackCommand command);

}
