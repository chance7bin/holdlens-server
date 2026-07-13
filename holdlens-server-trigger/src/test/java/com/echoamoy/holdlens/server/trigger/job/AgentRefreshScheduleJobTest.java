package com.echoamoy.holdlens.server.trigger.job;

import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.FundSliceRefreshCallbackCommand;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    public void schedulesUseShanghaiCalendarDefaultsAndHoldingRunsOnFirstAndFifteenth() throws Exception {
        Scheduled catalog = scheduled("runFundCatalogRefreshSchedule");
        Scheduled purchase = scheduled("runFundPurchaseStatusRefreshSchedule");
        Scheduled periodReturn = scheduled("runFundPeriodReturnRefreshSchedule");
        Scheduled holding = scheduled("runFundTopHoldingRefreshSchedule");
        Assert.assertEquals("Asia/Shanghai", catalog.zone());
        Assert.assertEquals("Asia/Shanghai", purchase.zone());
        Assert.assertEquals("Asia/Shanghai", periodReturn.zone());
        Assert.assertEquals("Asia/Shanghai", holding.zone());
        Assert.assertTrue(catalog.cron().contains("0 0 2 * * ?"));
        Assert.assertTrue(purchase.cron().contains("0 10 2 * * ?"));
        Assert.assertTrue(periodReturn.cron().contains("0 20 2 * * ?"));
        Assert.assertTrue(holding.cron().contains("0 30 2 1,15 * ?"));

        Field batchSize = AgentRefreshScheduleJob.class.getDeclaredField("holdingBatchSize");
        Assert.assertTrue(batchSize.getAnnotation(Value.class).value().endsWith(":20}"));
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

    private static class FakeCase implements IFundSliceRefreshCase {
        int calls;
        int lastBatchSize;
        public FundRefreshTaskResult scheduleCatalog(String trigger) { calls++; return null; }
        public FundRefreshTaskResult schedulePurchaseStatus(String trigger) { calls++; return null; }
        public FundRefreshTaskResult schedulePeriodReturn(String trigger) { calls++; return null; }
        public List<FundRefreshTaskResult> scheduleTopHoldings(String trigger, int batchSize) { calls++; lastBatchSize = batchSize; return List.of(); }
        public FundRefreshTaskResult dispatchTopHoldings(List<String> fundCodes, String trigger) { return null; }
        public FundRefreshTaskResult handleCallback(String taskType, FundSliceRefreshCallbackCommand command) { return null; }
        public int closeTimedOutCallbacks(int timeoutMinutes) { return 0; }
    }
}
