package com.echoamoy.holdlens.server.trigger.job;

import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 基金切片刷新触发器。这里只处理开关和 cron 路由，目标选择、批次和派发均由 Case 编排。
 */
@Slf4j
@Component
public class AgentRefreshScheduleJob {
    private static final String TRIGGER = "schedule";

    @Resource private IFundSliceRefreshCase fundSliceRefreshCase;
    @Value("${holdlens.agent.fund-catalog-refresh-schedule.enabled:false}") private boolean catalogEnabled;
    @Value("${holdlens.agent.fund-purchase-status-refresh-schedule.enabled:false}") private boolean purchaseEnabled;
    @Value("${holdlens.agent.fund-period-return-refresh-schedule.enabled:false}") private boolean returnEnabled;
    @Value("${holdlens.agent.fund-top-holding-refresh-schedule.enabled:false}") private boolean holdingEnabled;
    @Value("${holdlens.agent.fund-top-holding-refresh-schedule.batch-size:20}") private int holdingBatchSize;
    @Value("${holdlens.agent.fund-slice-callback-timeout.enabled:false}") private boolean callbackTimeoutEnabled;
    @Value("${holdlens.agent.fund-slice-callback-timeout.minutes:30}") private int callbackTimeoutMinutes;

    @Scheduled(cron="${holdlens.agent.fund-catalog-refresh-schedule.cron:0 0 2 * * ?}", zone="Asia/Shanghai")
    public void runFundCatalogRefreshSchedule() {
        if (catalogEnabled) fundSliceRefreshCase.scheduleCatalog(TRIGGER);
    }

    @Scheduled(cron="${holdlens.agent.fund-purchase-status-refresh-schedule.cron:0 10 2 * * ?}", zone="Asia/Shanghai")
    public void runFundPurchaseStatusRefreshSchedule() {
        if (purchaseEnabled) fundSliceRefreshCase.schedulePurchaseStatus(TRIGGER);
    }

    @Scheduled(cron="${holdlens.agent.fund-period-return-refresh-schedule.cron:0 20 2 * * ?}", zone="Asia/Shanghai")
    public void runFundPeriodReturnRefreshSchedule() {
        if (returnEnabled) fundSliceRefreshCase.schedulePeriodReturn(TRIGGER);
    }

    @Scheduled(cron="${holdlens.agent.fund-top-holding-refresh-schedule.cron:0 30 2 1,15 * ?}", zone="Asia/Shanghai")
    public void runFundTopHoldingRefreshSchedule() {
        if (!holdingEnabled) return;
        if (holdingBatchSize <= 0) {
            log.warn("跳过基金重仓刷新，batch-size 无效 batchSize={}", holdingBatchSize);
            return;
        }
        fundSliceRefreshCase.scheduleTopHoldings(TRIGGER, holdingBatchSize);
    }

    @Scheduled(cron="${holdlens.agent.fund-slice-callback-timeout.cron:0 */5 * * * ?}", zone="Asia/Shanghai")
    public void closeTimedOutCallbacks() {
        if (callbackTimeoutEnabled) fundSliceRefreshCase.closeTimedOutCallbacks(callbackTimeoutMinutes);
    }
}
