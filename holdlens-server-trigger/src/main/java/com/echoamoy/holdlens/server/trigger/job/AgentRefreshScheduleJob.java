package com.echoamoy.holdlens.server.trigger.job;

import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.entity.FundRefreshTargetEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class AgentRefreshScheduleJob {

    private static final String SCHEDULE_TRIGGER = "schedule";
    private static final Set<String> CONTINUABLE_STATUS = Set.of("created", "running", "dispatched");

    @Resource
    private IAgentFundRefreshCase agentFundRefreshCase;

    @Resource
    private IFundDataRepository fundDataRepository;

    @Value("${holdlens.agent.fund-refresh-schedule.enabled}")
    private boolean fundRefreshScheduleEnabled;

    @Value("${holdlens.agent.fund-refresh-schedule.batch-size}")
    private int fundRefreshBatchSize;

    @Scheduled(cron = "${holdlens.agent.fund-refresh-schedule.cron}")
    public void runFundRefreshSchedule() {
        if (!fundRefreshScheduleEnabled) {
            return;
        }
        if (!isValidBatchSize(fundRefreshBatchSize, ProcessingTaskEntity.FUND_DETAIL_REFRESH)) {
            return;
        }
        if (agentFundRefreshCase.hasNonTerminalTask(ProcessingTaskEntity.FUND_DETAIL_REFRESH)) {
            log.info("跳过基金详情定时刷新，本轮开始前已有非终态任务");
            return;
        }

        Long lastId = 0L;
        int batchNo = 0;
        while (true) {
            List<FundRefreshTargetEntity> targets = fundDataRepository.queryRefreshTargetsAfterId(lastId, fundRefreshBatchSize);
            if (targets.isEmpty()) {
                log.info("基金详情定时刷新扫描完成 batchCount={}", batchNo);
                return;
            }
            batchNo++;
            FundRefreshTaskResult result = agentFundRefreshCase.createAndDispatch(FundRefreshCreateCommand.builder()
                    .fundCodes(targets.stream().map(FundRefreshTargetEntity::getFundCode).toList())
                    .trigger(SCHEDULE_TRIGGER)
                    .build());
            if (!isContinuable(result)) {
                log.warn("基金详情定时刷新批次异常，停止本轮 batchNo={} targetCount={} serverTaskId={} status={} error={}",
                        batchNo, targets.size(), safeTaskId(result), safeStatus(result), safeError(result));
                return;
            }
            lastId = targets.get(targets.size() - 1).getId();
        }
    }

    private boolean isValidBatchSize(int batchSize, String taskType) {
        if (batchSize > 0) {
            return true;
        }
        log.warn("跳过 agent 刷新定时任务，batch-size 无效 taskType={} batchSize={}", taskType, batchSize);
        return false;
    }

    private boolean isContinuable(FundRefreshTaskResult result) {
        return result != null && CONTINUABLE_STATUS.contains(result.getStatus());
    }

    private String safeTaskId(FundRefreshTaskResult result) {
        return result == null ? null : result.getServerTaskId();
    }

    private String safeStatus(FundRefreshTaskResult result) {
        return result == null ? null : result.getStatus();
    }

    private String safeError(FundRefreshTaskResult result) {
        return result == null ? null : result.getErrorSummary();
    }

}
