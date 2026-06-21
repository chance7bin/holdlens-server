package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.dto.FundRefreshTaskDTO;
import com.echoamoy.holdlens.server.api.request.AgentFundRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.AgentStockQuoteRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.FundRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.response.Response;

/**
 * Agent 基金明细刷新接口，负责创建刷新任务、查询任务状态以及接收 agent 刷新结果回调。
 */
public interface IAgentFundRefreshService {

    /**
     * 创建基金明细刷新任务，并下发给 agent 异步刷新公开基金数据。
     */
    Response<FundRefreshTaskDTO> createTask(FundRefreshCreateRequest request);

    /**
     * 根据 server 任务 ID 查询基金明细刷新任务的当前状态和诊断信息。
     */
    Response<FundRefreshTaskDTO> queryTask(String serverTaskId);

    /**
     * 接收 agent 回传的基金明细刷新结果，用于保存结构化基金数据并更新任务状态。
     */
    Response<FundRefreshTaskDTO> callback(String callbackHeader, AgentFundRefreshCallbackRequest request);

    /**
     * 从当前股票行情表创建股票行情刷新任务，并下发给 agent 异步刷新公开行情数据。
     */
    Response<FundRefreshTaskDTO> createStockQuoteTask();

    /**
     * 接收 agent 回传的股票行情刷新结果，用于保存当前股票行情并更新任务状态。
     */
    Response<FundRefreshTaskDTO> stockQuoteCallback(String callbackHeader, AgentStockQuoteRefreshCallbackRequest request);

}
