package com.echoamoy.holdlens.server.cases.agent;

import com.echoamoy.holdlens.server.cases.agent.model.AgentFundRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;

public interface IAgentFundRefreshCase {

    FundRefreshTaskResult createAndDispatch(FundRefreshCreateCommand command);

    FundRefreshTaskResult handleCallback(AgentFundRefreshCallbackCommand command);

    FundRefreshTaskResult queryTask(String serverTaskId);

}
