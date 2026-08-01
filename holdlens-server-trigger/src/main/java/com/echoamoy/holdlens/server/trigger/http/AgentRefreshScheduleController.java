package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IAgentRefreshScheduleService;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import com.echoamoy.holdlens.server.cases.marketdetail.IMarketDataRefreshScheduleCase;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping
public class AgentRefreshScheduleController implements IAgentRefreshScheduleService {
    private static final String TRIGGER = "manual";

    private final IFundSliceRefreshCase fundSliceRefreshCase;
    private final IMarketDataRefreshScheduleCase marketDataRefreshScheduleCase;
    private final int holdingBatchSize;
    private final int allocationBatchSize;
    private final int callbackTimeoutMinutes;
    private final int callbackProcessingWarningMinutes;

    public AgentRefreshScheduleController(
            IFundSliceRefreshCase fundSliceRefreshCase,
            IMarketDataRefreshScheduleCase marketDataRefreshScheduleCase,
            @Value("${holdlens.agent.fund-top-holding-refresh-schedule.batch-size}") int holdingBatchSize,
            @Value("${holdlens.agent.fund-asset-allocation-refresh-schedule.batch-size}") int allocationBatchSize,
            @Value("${holdlens.agent.fund-slice-callback-timeout.minutes}") int callbackTimeoutMinutes,
            @Value("${holdlens.agent.fund-slice-callback-timeout.processing-warning-minutes:10}")
            int callbackProcessingWarningMinutes) {
        this.fundSliceRefreshCase = fundSliceRefreshCase;
        this.marketDataRefreshScheduleCase = marketDataRefreshScheduleCase;
        this.holdingBatchSize = holdingBatchSize;
        this.allocationBatchSize = allocationBatchSize;
        this.callbackTimeoutMinutes = callbackTimeoutMinutes;
        this.callbackProcessingWarningMinutes = callbackProcessingWarningMinutes;
    }

    @PostMapping("/api/agent/fund-catalog-refresh/schedule-runs")
    @Override
    public Response<Void> runFundCatalogRefreshSchedule() {
        log.info("手动触发基金目录全量刷新调度");
        fundSliceRefreshCase.scheduleCatalog(TRIGGER);
        return Response.ok(null);
    }

    @PostMapping("/api/agent/fund-purchase-status-refresh/schedule-runs")
    @Override
    public Response<Void> runFundPurchaseStatusRefreshSchedule() {
        log.info("手动触发基金申购状态刷新调度");
        fundSliceRefreshCase.schedulePurchaseStatus(TRIGGER);
        return Response.ok(null);
    }

    @PostMapping("/api/agent/fund-top-holding-refresh/schedule-runs")
    @Override
    public Response<Void> runFundTopHoldingRefreshSchedule() {
        log.info("手动触发基金重仓刷新调度");
        fundSliceRefreshCase.scheduleTopHoldings(TRIGGER, holdingBatchSize);
        return Response.ok(null);
    }

    @PostMapping("/api/agent/fund-asset-allocation-refresh/schedule-runs")
    @Override
    public Response<Void> runFundAssetAllocationRefreshSchedule() {
        log.info("手动触发基金资产配置刷新调度");
        fundSliceRefreshCase.scheduleAssetAllocations(TRIGGER, allocationBatchSize);
        return Response.ok(null);
    }

    @PostMapping("/api/agent/fund-slice-callback-timeout/schedule-runs")
    @Override
    public Response<Void> runFundSliceCallbackTimeoutSchedule() {
        log.info("手动触发基金切片回调超时处理");
        fundSliceRefreshCase.closeTimedOutCallbacks(callbackTimeoutMinutes);
        fundSliceRefreshCase.warnSlowCatalogCallbacks(callbackProcessingWarningMinutes);
        return Response.ok(null);
    }

    @PostMapping("/api/agent/a-share-market-refresh/schedule-runs")
    @Override
    public Response<Void> runAShareMarketRefreshSchedule() {
        log.info("手动触发 A 股全市场刷新调度");
        marketDataRefreshScheduleCase.runAShareMarketRefresh();
        return Response.ok(null);
    }

    @PostMapping("/api/agent/us-stock-market-refresh/schedule-runs")
    @Override
    public Response<Void> runUSStockMarketRefreshSchedule() {
        log.info("手动触发美股全市场刷新调度");
        marketDataRefreshScheduleCase.runUSStockMarketRefresh();
        return Response.ok(null);
    }

    @PostMapping("/api/agent/active-fund-detail-refresh/schedule-runs")
    @Override
    public Response<Void> runActiveFundDetailRefreshSchedule() {
        log.info("手动触发活跃基金详情刷新调度");
        marketDataRefreshScheduleCase.runFundDetailRefresh();
        return Response.ok(null);
    }

    @PostMapping("/api/agent/active-a-share-detail-refresh/schedule-runs")
    @Override
    public Response<Void> runActiveAShareDetailRefreshSchedule() {
        log.info("手动触发活跃 A 股详情刷新调度");
        marketDataRefreshScheduleCase.runStockDetailRefresh(StockMarketEntity.MARKET_A_SHARE);
        return Response.ok(null);
    }

    @PostMapping("/api/agent/active-us-stock-detail-refresh/schedule-runs")
    @Override
    public Response<Void> runActiveUSStockDetailRefreshSchedule() {
        log.info("手动触发活跃美股详情刷新调度");
        marketDataRefreshScheduleCase.runStockDetailRefresh(StockMarketEntity.MARKET_US_STOCK);
        return Response.ok(null);
    }

}
