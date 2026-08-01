package com.echoamoy.holdlens.server.trigger.job;

import com.echoamoy.holdlens.server.cases.marketdetail.IMarketDataRefreshScheduleCase;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

public class MarketDataRefreshScheduleJobTest {

    @Test
    public void disabledSchedulesDoNotDelegateAndEnabledSchedulesRouteByMarket() throws Exception {
        FakeScheduleCase fake = new FakeScheduleCase();
        MarketDataRefreshScheduleJob job = newJob(fake);

        job.runAShareMarketRefresh();
        job.runUSStockMarketRefresh();
        job.runFundDetailRefresh();
        job.runAShareDetailRefresh();
        job.runUSStockDetailRefresh();
        Assert.assertEquals(0, fake.calls);

        set(job, "aShareEnabled", true);
        set(job, "usStockEnabled", true);
        set(job, "fundDetailEnabled", true);
        set(job, "stockDetailEnabled", true);
        job.runAShareMarketRefresh();
        job.runUSStockMarketRefresh();
        job.runFundDetailRefresh();
        job.runAShareDetailRefresh();
        job.runUSStockDetailRefresh();
        Assert.assertEquals(5, fake.calls);
        Assert.assertEquals("US_STOCK", fake.lastMarket);
    }

    @Test
    public void schedulesAndDefaultConfigurationAreExternalized() throws Exception {
        assertSchedule("runAShareMarketRefresh", "${holdlens.agent.a-share-market-refresh-schedule.cron}",
                "${holdlens.agent.a-share-market-refresh-schedule.zone}");
        assertSchedule("runUSStockMarketRefresh", "${holdlens.agent.us-stock-market-refresh-schedule.cron}",
                "${holdlens.agent.us-stock-market-refresh-schedule.zone}");
        assertSchedule("runFundDetailRefresh", "${holdlens.agent.active-fund-detail-refresh-schedule.cron}",
                "${holdlens.agent.active-fund-detail-refresh-schedule.zone}");
        assertSchedule("runAShareDetailRefresh", "${holdlens.agent.active-a-share-detail-refresh-schedule.cron}",
                "${holdlens.agent.active-a-share-detail-refresh-schedule.zone}");
        assertSchedule("runUSStockDetailRefresh", "${holdlens.agent.active-us-stock-detail-refresh-schedule.cron}",
                "${holdlens.agent.active-us-stock-detail-refresh-schedule.zone}");

        PropertySource<?> properties = new YamlPropertySourceLoader().load("application",
                new FileSystemResource(projectRoot().resolve("holdlens-server-app/src/main/resources/application.yml"))).get(0);
        Assert.assertEquals(false, properties.getProperty("holdlens.agent.a-share-market-refresh-schedule.enabled"));
        Assert.assertEquals(false, properties.getProperty("holdlens.agent.us-stock-market-refresh-schedule.enabled"));
        Assert.assertEquals(false, properties.getProperty("holdlens.agent.active-fund-detail-refresh-schedule.enabled"));
        Assert.assertEquals(false, properties.getProperty("holdlens.agent.active-stock-detail-refresh-schedule.enabled"));
        Assert.assertEquals("0 30 15 ? * MON-FRI", properties.getProperty(
                "holdlens.agent.a-share-market-refresh-schedule.cron"));
        Assert.assertEquals("0 15 13,16 ? * MON-FRI", properties.getProperty(
                "holdlens.agent.us-stock-market-refresh-schedule.cron"));
        Assert.assertEquals("0 30 22 ? * MON-FRI", properties.getProperty(
                "holdlens.agent.active-fund-detail-refresh-schedule.cron"));
        Assert.assertEquals("0 45 15 ? * MON-FRI", properties.getProperty(
                "holdlens.agent.active-a-share-detail-refresh-schedule.cron"));
        Assert.assertEquals("0 30 16 ? * MON-FRI", properties.getProperty(
                "holdlens.agent.active-us-stock-detail-refresh-schedule.cron"));
        Assert.assertEquals(20, properties.getProperty("holdlens.market-detail.fund-nav-stale-hours"));
        Assert.assertEquals(168, properties.getProperty("holdlens.market-detail.fund-performance-stale-hours"));
        Assert.assertEquals(20, properties.getProperty("holdlens.market-detail.stock-price-stale-hours"));
        Assert.assertEquals(30, properties.getProperty("holdlens.market-detail.stock-profile-stale-days"));
        Assert.assertNotNull(CronExpression.parse((String) properties.getProperty(
                "holdlens.agent.a-share-market-refresh-schedule.cron")));
        Assert.assertNotNull(CronExpression.parse((String) properties.getProperty(
                "holdlens.agent.us-stock-market-refresh-schedule.cron")));
    }

    private void assertSchedule(String methodName, String cron, String zone) throws Exception {
        Method method = MarketDataRefreshScheduleJob.class.getDeclaredMethod(methodName);
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        Assert.assertEquals(cron, scheduled.cron());
        Assert.assertEquals(zone, scheduled.zone());
    }

    private MarketDataRefreshScheduleJob newJob(FakeScheduleCase fake) throws Exception {
        MarketDataRefreshScheduleJob job = new MarketDataRefreshScheduleJob();
        set(job, "scheduleCase", fake);
        return job;
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Path projectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("holdlens-server-app/pom.xml"))) {
            current = current.getParent();
        }
        if (current == null) throw new IllegalStateException("cannot locate holdlens-server project root");
        return current;
    }

    private static class FakeScheduleCase implements IMarketDataRefreshScheduleCase {
        int calls;
        String lastMarket;
        @Override public boolean runAShareMarketRefresh() { calls++; return true; }
        @Override public boolean runUSStockMarketRefresh() { calls++; return true; }
        @Override public int runFundDetailRefresh() { calls++; return 1; }
        @Override public int runStockDetailRefresh(String market) { calls++; lastMarket = market; return 1; }
    }
}
