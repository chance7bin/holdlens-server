package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IAgentRefreshScheduleService;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
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
    private final int holdingBatchSize;
    private final int allocationBatchSize;

    public AgentRefreshScheduleController(
            IFundSliceRefreshCase fundSliceRefreshCase,
            @Value("${holdlens.agent.fund-top-holding-refresh-schedule.batch-size}") int holdingBatchSize,
            @Value("${holdlens.agent.fund-asset-allocation-refresh-schedule.batch-size}") int allocationBatchSize) {
        this.fundSliceRefreshCase = fundSliceRefreshCase;
        this.holdingBatchSize = holdingBatchSize;
        this.allocationBatchSize = allocationBatchSize;
    }

    @PostMapping("/api/agent/fund-catalog-refresh/schedule-runs")
    @Override
    public Response<Void> runFundCatalogRefreshSchedule() {
        log.info("手动触发基金目录全量刷新调度");
        fundSliceRefreshCase.scheduleCatalog(TRIGGER);
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

}
