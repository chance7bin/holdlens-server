package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.response.Response;

/**
 * Agent 刷新调度接口，负责手动触发现有定时刷新逻辑。
 */
public interface IAgentRefreshScheduleService {

    /**
     * 手动触发基金目录全量刷新调度，执行规则与 cron 定时触发保持一致。
     */
    Response<Void> runFundCatalogRefreshSchedule();

    /**
     * 手动触发基金重仓刷新调度，执行规则与 cron 定时触发保持一致。
     */
    Response<Void> runFundTopHoldingRefreshSchedule();

    /**
     * 手动触发基金资产配置刷新调度，执行规则与 cron 定时触发保持一致。
     */
    Response<Void> runFundAssetAllocationRefreshSchedule();

}
