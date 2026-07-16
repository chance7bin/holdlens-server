package com.echoamoy.holdlens.server.api;

import com.echoamoy.holdlens.server.api.response.Response;

/**
 * Agent 刷新调度接口，负责手动触发现有刷新逻辑。
 */
public interface IAgentRefreshScheduleService {

    /**
     * 手动触发基金目录全量刷新，不受 cron 定时开关影响。
     */
    Response<Void> runFundCatalogRefreshSchedule();

    /**
     * 手动触发基金重仓刷新，不受 cron 定时开关影响。
     */
    Response<Void> runFundTopHoldingRefreshSchedule();

    /**
     * 手动触发基金资产配置刷新，不受 cron 定时开关影响。
     */
    Response<Void> runFundAssetAllocationRefreshSchedule();

}
