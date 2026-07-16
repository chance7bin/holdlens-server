package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.trigger.job.AgentRefreshScheduleJob;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;

public class AgentRefreshScheduleControllerTest {

    @Test
    public void testRunFundCatalogRefreshSchedule() {
        CountingAgentRefreshScheduleJob job = new CountingAgentRefreshScheduleJob();
        AgentRefreshScheduleController controller = new AgentRefreshScheduleController(job);

        Response<Void> response = controller.runFundCatalogRefreshSchedule();

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals("成功", response.getInfo());
        Assert.assertNull(response.getData());
        Assert.assertEquals(1, job.fundCatalogRunCount);
    }

    @Test
    public void testFundCatalogRefreshScheduleMapping() throws Exception {
        Method method = AgentRefreshScheduleController.class.getMethod("runFundCatalogRefreshSchedule");
        PostMapping mapping = method.getAnnotation(PostMapping.class);

        Assert.assertNotNull(mapping);
        Assert.assertArrayEquals(new String[]{"/api/agent/fund-catalog-refresh/schedule-runs"}, mapping.value());
    }

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
        private int fundCatalogRunCount;
        private int fundRunCount;

        @Override
        public void runFundCatalogRefreshSchedule() {
            fundCatalogRunCount++;
        }

        @Override
        public void runFundTopHoldingRefreshSchedule() {
            fundRunCount++;
        }
    }

}
