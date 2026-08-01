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
     * 手动触发基金申购状态刷新，不受 cron 定时开关影响。
     */
    Response<Void> runFundPurchaseStatusRefreshSchedule();

    /**
     * 手动触发基金重仓刷新，不受 cron 定时开关影响。
     */
    Response<Void> runFundTopHoldingRefreshSchedule();

    /**
     * 手动触发基金资产配置刷新，不受 cron 定时开关影响。
     */
    Response<Void> runFundAssetAllocationRefreshSchedule();

    /**
     * 手动触发基金切片回调超时处理，不受 cron 定时开关影响。
     */
    Response<Void> runFundSliceCallbackTimeoutSchedule();

    /**
     * 手动触发 A 股全市场刷新，不受 cron 定时开关影响。
     */
    Response<Void> runAShareMarketRefreshSchedule();

    /**
     * 手动触发美股全市场刷新，不受 cron 定时开关影响。
     */
    Response<Void> runUSStockMarketRefreshSchedule();

    /**
     * 手动触发活跃基金详情刷新，不受 cron 定时开关影响。
     */
    Response<Void> runActiveFundDetailRefreshSchedule();

    /**
     * 手动触发活跃 A 股详情刷新，不受 cron 定时开关影响。
     */
    Response<Void> runActiveAShareDetailRefreshSchedule();

    /**
     * 手动触发活跃美股详情刷新，不受 cron 定时开关影响。
     */
    Response<Void> runActiveUSStockDetailRefreshSchedule();

}
