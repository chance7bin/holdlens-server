package com.echoamoy.holdlens.server.trigger.job;

import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.FundSliceRefreshCallbackCommand;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AgentRefreshScheduleJobTest {

    @Test
    public void allSchedulesAreDisabledIndependently() throws Exception {
        FakeCase fake = new FakeCase();
        AgentRefreshScheduleJob job = newJob(fake);
        job.runFundCatalogRefreshSchedule();
        job.runFundPurchaseStatusRefreshSchedule();
        job.runFundPeriodReturnRefreshSchedule();
        job.runFundTopHoldingRefreshSchedule();
        Assert.assertEquals(0, fake.calls);
    }

    @Test
    public void enabledSchedulesOnlyRouteToCase() throws Exception {
        FakeCase fake = new FakeCase();
        AgentRefreshScheduleJob job = newJob(fake);
        setField(job, "catalogEnabled", true);
        setField(job, "purchaseEnabled", true);
        setField(job, "returnEnabled", true);
        setField(job, "holdingEnabled", true);
        setField(job, "holdingBatchSize", 20);
        job.runFundCatalogRefreshSchedule();
        job.runFundPurchaseStatusRefreshSchedule();
        job.runFundPeriodReturnRefreshSchedule();
        job.runFundTopHoldingRefreshSchedule();
        Assert.assertEquals(4, fake.calls);
        Assert.assertEquals(20, fake.lastBatchSize);
    }

    @Test
    public void invalidHoldingBatchIsSafelySkipped() throws Exception {
        FakeCase fake = new FakeCase();
        AgentRefreshScheduleJob job = newJob(fake);
        setField(job, "holdingEnabled", true);
        setField(job, "holdingBatchSize", 0);
        job.runFundTopHoldingRefreshSchedule();
        Assert.assertEquals(0, fake.calls);
    }

    @Test
    public void callbackTimeoutScheduleClosesMissingCallbacksAndWarnsSlowCatalogProcessing() throws Exception {
        FakeCase fake = new FakeCase();
        AgentRefreshScheduleJob job = newJob(fake);
        setField(job, "callbackTimeoutEnabled", true);
        setField(job, "callbackTimeoutMinutes", 30);
        setField(job, "callbackProcessingWarningMinutes", 10);

        job.closeTimedOutCallbacks();

        Assert.assertEquals(30, fake.lastCallbackTimeoutMinutes);
        Assert.assertEquals(10, fake.lastProcessingWarningMinutes);
    }

    @Test
    public void schedulesReferenceExternalConfiguration() throws Exception {
        Scheduled catalog = scheduled("runFundCatalogRefreshSchedule");
        Scheduled purchase = scheduled("runFundPurchaseStatusRefreshSchedule");
        Scheduled periodReturn = scheduled("runFundPeriodReturnRefreshSchedule");
        Scheduled holding = scheduled("runFundTopHoldingRefreshSchedule");
        Scheduled callbackTimeout = scheduled("closeTimedOutCallbacks");
        assertSchedule(catalog, "${holdlens.agent.fund-catalog-refresh-schedule.cron}");
        assertSchedule(purchase, "${holdlens.agent.fund-purchase-status-refresh-schedule.cron}");
        assertSchedule(periodReturn, "${holdlens.agent.fund-period-return-refresh-schedule.cron}");
        assertSchedule(holding, "${holdlens.agent.fund-top-holding-refresh-schedule.cron}");
        assertSchedule(callbackTimeout, "${holdlens.agent.fund-slice-callback-timeout.cron}");

        assertValue("catalogEnabled", "${holdlens.agent.fund-catalog-refresh-schedule.enabled}");
        assertValue("purchaseEnabled", "${holdlens.agent.fund-purchase-status-refresh-schedule.enabled}");
        assertValue("returnEnabled", "${holdlens.agent.fund-period-return-refresh-schedule.enabled}");
        assertValue("holdingEnabled", "${holdlens.agent.fund-top-holding-refresh-schedule.enabled}");
        assertValue("holdingBatchSize", "${holdlens.agent.fund-top-holding-refresh-schedule.batch-size}");
        assertValue("callbackTimeoutEnabled", "${holdlens.agent.fund-slice-callback-timeout.enabled}");
        assertValue("callbackTimeoutMinutes", "${holdlens.agent.fund-slice-callback-timeout.minutes}");
        assertValue("callbackProcessingWarningMinutes", "${holdlens.agent.fund-slice-callback-timeout.processing-warning-minutes:10}");
    }

    @Test
    public void commonConfigurationKeepsRefreshSchedulesDisabledByDefault() throws Exception {
        Path applicationConfig = projectRoot().resolve("holdlens-server-app/src/main/resources/application.yml");
        PropertySource<?> properties = new YamlPropertySourceLoader()
                .load("application", new FileSystemResource(applicationConfig))
                .get(0);

        Assert.assertEquals("Asia/Shanghai", properties.getProperty("holdlens.agent.fund-refresh-schedule-zone"));
        Assert.assertEquals(false, properties.getProperty("holdlens.agent.fund-catalog-refresh-schedule.enabled"));
        Assert.assertEquals(false, properties.getProperty("holdlens.agent.fund-purchase-status-refresh-schedule.enabled"));
        Assert.assertEquals(false, properties.getProperty("holdlens.agent.fund-period-return-refresh-schedule.enabled"));
        Assert.assertEquals(false, properties.getProperty("holdlens.agent.fund-top-holding-refresh-schedule.enabled"));
        Assert.assertEquals(false, properties.getProperty("holdlens.agent.fund-slice-callback-timeout.enabled"));
        Assert.assertEquals(10, properties.getProperty("holdlens.agent.fund-slice-callback-timeout.processing-warning-minutes"));
    }

    private void assertSchedule(Scheduled scheduled, String cron) {
        Assert.assertEquals(cron, scheduled.cron());
        Assert.assertEquals("${holdlens.agent.fund-refresh-schedule-zone}", scheduled.zone());
    }

    private void assertValue(String fieldName, String property) throws Exception {
        Field field = AgentRefreshScheduleJob.class.getDeclaredField(fieldName);
        Assert.assertEquals(property, field.getAnnotation(Value.class).value());
    }

    private Scheduled scheduled(String methodName) throws Exception {
        Method method = AgentRefreshScheduleJob.class.getDeclaredMethod(methodName);
        return method.getAnnotation(Scheduled.class);
    }

    private AgentRefreshScheduleJob newJob(FakeCase fake) throws Exception {
        AgentRefreshScheduleJob job = new AgentRefreshScheduleJob();
        setField(job, "fundSliceRefreshCase", fake);
        return job;
    }

    private void setField(Object target, String name, Object value) throws Exception {
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

    private static class FakeCase implements IFundSliceRefreshCase {
        int calls;
        int lastBatchSize;
        int lastCallbackTimeoutMinutes;
        int lastProcessingWarningMinutes;
        public FundRefreshTaskResult scheduleCatalog(String trigger) { calls++; return null; }
        public FundRefreshTaskResult schedulePurchaseStatus(String trigger) { calls++; return null; }
        public FundRefreshTaskResult schedulePeriodReturn(String trigger) { calls++; return null; }
        public List<FundRefreshTaskResult> scheduleTopHoldings(String trigger, int batchSize) { calls++; lastBatchSize = batchSize; return List.of(); }
        public FundRefreshTaskResult dispatchTopHoldings(List<String> fundCodes, String trigger) { return null; }
        public FundRefreshTaskResult handleCallback(String taskType, FundSliceRefreshCallbackCommand command) { return null; }
        public int closeTimedOutCallbacks(int timeoutMinutes) { lastCallbackTimeoutMinutes = timeoutMinutes; return 0; }
        public int warnSlowCatalogCallbacks(int warningMinutes) { lastProcessingWarningMinutes = warningMinutes; return 0; }
    }
}
