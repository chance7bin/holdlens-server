package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IAgentRefreshScheduleService;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.trigger.job.AgentRefreshScheduleJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping
public class AgentRefreshScheduleController implements IAgentRefreshScheduleService {

    private final AgentRefreshScheduleJob agentRefreshScheduleJob;

    public AgentRefreshScheduleController(AgentRefreshScheduleJob agentRefreshScheduleJob) {
        this.agentRefreshScheduleJob = agentRefreshScheduleJob;
    }

    @PostMapping("/api/agent/fund-catalog-refresh/schedule-runs")
    @Override
    public Response<Void> runFundCatalogRefreshSchedule() {
        log.info("手动触发基金目录全量刷新调度");
        agentRefreshScheduleJob.runFundCatalogRefreshSchedule();
        return Response.ok(null);
    }

    @PostMapping("/api/agent/fund-top-holding-refresh/schedule-runs")
    @Override
    public Response<Void> runFundTopHoldingRefreshSchedule() {
        log.info("手动触发基金重仓刷新调度");
        agentRefreshScheduleJob.runFundTopHoldingRefreshSchedule();
        return Response.ok(null);
    }

}
