package com.echoamoy.holdlens.server.trigger.job;

import com.echoamoy.holdlens.server.cases.marketdetail.IMarketDataRefreshScheduleCase;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketDataRefreshScheduleJob {

    @Resource private IMarketDataRefreshScheduleCase scheduleCase;
    @Value("${holdlens.agent.a-share-market-refresh-schedule.enabled:false}") private boolean aShareEnabled;
    @Value("${holdlens.agent.us-stock-market-refresh-schedule.enabled:false}") private boolean usStockEnabled;
    @Value("${holdlens.agent.active-fund-detail-refresh-schedule.enabled:false}") private boolean fundDetailEnabled;
    @Value("${holdlens.agent.active-stock-detail-refresh-schedule.enabled:false}") private boolean stockDetailEnabled;

    @Scheduled(cron = "${holdlens.agent.a-share-market-refresh-schedule.cron}",
            zone = "${holdlens.agent.a-share-market-refresh-schedule.zone}")
    public void runAShareMarketRefresh() {
        if (aShareEnabled) scheduleCase.runAShareMarketRefresh();
    }

    @Scheduled(cron = "${holdlens.agent.us-stock-market-refresh-schedule.cron}",
            zone = "${holdlens.agent.us-stock-market-refresh-schedule.zone}")
    public void runUSStockMarketRefresh() {
        if (usStockEnabled) scheduleCase.runUSStockMarketRefresh();
    }

    @Scheduled(cron = "${holdlens.agent.active-fund-detail-refresh-schedule.cron}",
            zone = "${holdlens.agent.active-fund-detail-refresh-schedule.zone}")
    public void runFundDetailRefresh() {
        if (fundDetailEnabled) scheduleCase.runFundDetailRefresh();
    }

    @Scheduled(cron = "${holdlens.agent.active-a-share-detail-refresh-schedule.cron}",
            zone = "${holdlens.agent.active-a-share-detail-refresh-schedule.zone}")
    public void runAShareDetailRefresh() {
        if (stockDetailEnabled) scheduleCase.runStockDetailRefresh(StockMarketEntity.MARKET_A_SHARE);
    }

    @Scheduled(cron = "${holdlens.agent.active-us-stock-detail-refresh-schedule.cron}",
            zone = "${holdlens.agent.active-us-stock-detail-refresh-schedule.zone}")
    public void runUSStockDetailRefresh() {
        if (stockDetailEnabled) scheduleCase.runStockDetailRefresh(StockMarketEntity.MARKET_US_STOCK);
    }
}
