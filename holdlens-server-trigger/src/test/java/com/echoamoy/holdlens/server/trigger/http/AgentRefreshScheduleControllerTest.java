package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.trigger.job.AgentRefreshScheduleJob;
import org.junit.Assert;
import org.junit.Test;

public class AgentRefreshScheduleControllerTest {

    @Test
    public void testRunFundRefreshSchedule() {
        CountingAgentRefreshScheduleJob job = new CountingAgentRefreshScheduleJob();
        AgentRefreshScheduleController controller = new AgentRefreshScheduleController(job);

        Response<Void> response = controller.runFundRefreshSchedule();

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals("成功", response.getInfo());
        Assert.assertNull(response.getData());
        Assert.assertEquals(1, job.fundRunCount);
        Assert.assertEquals(0, job.stockRunCount);
    }

    @Test
    public void testRunStockRefreshSchedule() {
        CountingAgentRefreshScheduleJob job = new CountingAgentRefreshScheduleJob();
        AgentRefreshScheduleController controller = new AgentRefreshScheduleController(job);

        Response<Void> response = controller.runStockRefreshSchedule();

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals("成功", response.getInfo());
        Assert.assertNull(response.getData());
        Assert.assertEquals(0, job.fundRunCount);
        Assert.assertEquals(1, job.stockRunCount);
    }

    private static class CountingAgentRefreshScheduleJob extends AgentRefreshScheduleJob {
        private int fundRunCount;
        private int stockRunCount;

        @Override
        public void runFundRefreshSchedule() {
            fundRunCount++;
        }

        @Override
        public void runStockRefreshSchedule() {
            stockRunCount++;
        }
    }

}
