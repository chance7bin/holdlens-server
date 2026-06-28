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

    @PostMapping("/api/agent/fund-detail-refresh/schedule-runs")
    @Override
    public Response<Void> runFundRefreshSchedule() {
        log.info("手动触发基金详情刷新调度");
        agentRefreshScheduleJob.runFundRefreshSchedule();
        return Response.ok(null);
    }

    @PostMapping("/api/agent/stock-quote-refresh/schedule-runs")
    @Override
    public Response<Void> runStockRefreshSchedule() {
        log.info("手动触发股票行情刷新调度");
        agentRefreshScheduleJob.runStockRefreshSchedule();
        return Response.ok(null);
    }

}
