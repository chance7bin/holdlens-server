package com.echoamoy.holdlens.server.cases.agent;

import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.FundSliceRefreshCallbackCommand;

import java.util.List;

public interface IFundSliceRefreshCase {
    FundRefreshTaskResult scheduleCatalog(String trigger);
    FundRefreshTaskResult schedulePurchaseStatus(String trigger);
    FundRefreshTaskResult schedulePeriodReturn(String trigger);
    List<FundRefreshTaskResult> scheduleTopHoldings(String trigger, int batchSize);
    FundRefreshTaskResult dispatchTopHoldings(List<String> fundCodes, String trigger);
    FundRefreshTaskResult handleCallback(String taskType, FundSliceRefreshCallbackCommand command);
    int closeTimedOutCallbacks(int timeoutMinutes);
}
