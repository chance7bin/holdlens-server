package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.trigger.job.AgentRefreshScheduleJob;
import org.junit.Assert;
import org.junit.Test;

public class AgentRefreshScheduleControllerTest {

    @Test
    public void testRunFundTopHoldingRefreshSchedule() {
        CountingAgentRefreshScheduleJob job = new CountingAgentRefreshScheduleJob();
        AgentRefreshScheduleController controller = new AgentRefreshScheduleController(job);

        Response<Void> response = controller.runFundTopHoldingRefreshSchedule();

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals("成功", response.getInfo());
        Assert.assertNull(response.getData());
        Assert.assertEquals(1, job.fundRunCount);
    }

    private static class CountingAgentRefreshScheduleJob extends AgentRefreshScheduleJob {
        private int fundRunCount;

        @Override
        public void runFundTopHoldingRefreshSchedule() {
            fundRunCount++;
        }
    }

}
