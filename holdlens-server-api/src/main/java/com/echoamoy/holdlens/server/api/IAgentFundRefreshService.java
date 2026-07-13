package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.dto.FundRefreshTaskDTO;
import com.echoamoy.holdlens.server.api.request.AShareMarketRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.AShareMarketRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.request.USStockMarketRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.USStockMarketRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.response.Response;

/**
 * Agent 股票市场刷新接口。
 */
public interface IAgentFundRefreshService {

    /**
     * 创建 A 股全量行情刷新任务，并下发给 agent 异步刷新公开行情数据。
     */
    Response<FundRefreshTaskDTO> createAShareMarketTask(AShareMarketRefreshCreateRequest request);

    /**
     * 接收 agent 回传的 A 股全量行情刷新结果，用于保存当前股票行情并更新任务状态。
     */
    Response<FundRefreshTaskDTO> aShareMarketCallback(String callbackHeader, AShareMarketRefreshCallbackRequest request);

    /**
     * 创建美股全量行情刷新任务，并下发给 agent 异步刷新公开行情数据。
     */
    Response<FundRefreshTaskDTO> createUSStockMarketTask(USStockMarketRefreshCreateRequest request);

    /**
     * 接收 agent 回传的美股全量行情刷新结果，用于保存当前股票行情并更新任务状态。
     */
    Response<FundRefreshTaskDTO> usStockMarketCallback(String callbackHeader, USStockMarketRefreshCallbackRequest request);

}
