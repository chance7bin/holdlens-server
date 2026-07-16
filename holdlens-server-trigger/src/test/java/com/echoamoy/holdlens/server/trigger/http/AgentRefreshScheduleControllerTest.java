package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.FundSliceRefreshCallbackCommand;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.List;

public class AgentRefreshScheduleControllerTest {

    @Test
    public void testRunFundCatalogRefreshSchedule() {
        RecordingFundSliceRefreshCase refreshCase = new RecordingFundSliceRefreshCase();
        AgentRefreshScheduleController controller = newController(refreshCase);

        Response<Void> response = controller.runFundCatalogRefreshSchedule();

        assertSuccess(response);
        Assert.assertEquals(1, refreshCase.fundCatalogRunCount);
        Assert.assertEquals("manual", refreshCase.fundCatalogTrigger);
    }

    @Test
    public void testFundCatalogRefreshScheduleMapping() throws Exception {
        assertMapping("runFundCatalogRefreshSchedule", "/api/agent/fund-catalog-refresh/schedule-runs");
    }

    @Test
    public void testRunFundTopHoldingRefreshSchedule() {
        RecordingFundSliceRefreshCase refreshCase = new RecordingFundSliceRefreshCase();
        AgentRefreshScheduleController controller = newController(refreshCase);

        Response<Void> response = controller.runFundTopHoldingRefreshSchedule();

        assertSuccess(response);
        Assert.assertEquals(1, refreshCase.fundTopHoldingRunCount);
        Assert.assertEquals("manual", refreshCase.fundTopHoldingTrigger);
        Assert.assertEquals(20, refreshCase.fundTopHoldingBatchSize);
    }

    @Test
    public void testFundTopHoldingRefreshScheduleMapping() throws Exception {
        assertMapping("runFundTopHoldingRefreshSchedule", "/api/agent/fund-top-holding-refresh/schedule-runs");
    }

    @Test
    public void testRunFundAssetAllocationRefreshSchedule() {
        RecordingFundSliceRefreshCase refreshCase = new RecordingFundSliceRefreshCase();
        AgentRefreshScheduleController controller = newController(refreshCase);

        Response<Void> response = controller.runFundAssetAllocationRefreshSchedule();

        assertSuccess(response);
        Assert.assertEquals(1, refreshCase.fundAssetAllocationRunCount);
        Assert.assertEquals("manual", refreshCase.fundAssetAllocationTrigger);
        Assert.assertEquals(30, refreshCase.fundAssetAllocationBatchSize);
    }

    @Test
    public void testFundAssetAllocationRefreshScheduleMapping() throws Exception {
        assertMapping("runFundAssetAllocationRefreshSchedule", "/api/agent/fund-asset-allocation-refresh/schedule-runs");
    }

    private AgentRefreshScheduleController newController(RecordingFundSliceRefreshCase refreshCase) {
        return new AgentRefreshScheduleController(refreshCase, 20, 30);
    }

    private void assertSuccess(Response<Void> response) {
        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals("成功", response.getInfo());
        Assert.assertNull(response.getData());
    }

    private void assertMapping(String methodName, String path) throws Exception {
        Method method = AgentRefreshScheduleController.class.getMethod(methodName);
        PostMapping mapping = method.getAnnotation(PostMapping.class);

        Assert.assertNotNull(mapping);
        Assert.assertArrayEquals(new String[]{path}, mapping.value());
    }

    private static class RecordingFundSliceRefreshCase implements IFundSliceRefreshCase {
        private int fundCatalogRunCount;
        private String fundCatalogTrigger;
        private int fundTopHoldingRunCount;
        private String fundTopHoldingTrigger;
        private int fundTopHoldingBatchSize;
        private int fundAssetAllocationRunCount;
        private String fundAssetAllocationTrigger;
        private int fundAssetAllocationBatchSize;

        @Override
        public FundRefreshTaskResult scheduleCatalog(String trigger) {
            fundCatalogRunCount++;
            fundCatalogTrigger = trigger;
            return null;
        }

        @Override
        public FundRefreshTaskResult schedulePurchaseStatus(String trigger) {
            return null;
        }

        @Override
        public FundRefreshTaskResult schedulePeriodReturn(String trigger) {
            return null;
        }

        @Override
        public List<FundRefreshTaskResult> scheduleTopHoldings(String trigger, int batchSize) {
            fundTopHoldingRunCount++;
            fundTopHoldingTrigger = trigger;
            fundTopHoldingBatchSize = batchSize;
            return List.of();
        }

        @Override
        public List<FundRefreshTaskResult> scheduleAssetAllocations(String trigger, int batchSize) {
            fundAssetAllocationRunCount++;
            fundAssetAllocationTrigger = trigger;
            fundAssetAllocationBatchSize = batchSize;
            return List.of();
        }

        @Override
        public FundRefreshTaskResult dispatchTopHoldings(List<String> fundCodes, String trigger) {
            return null;
        }

        @Override
        public FundRefreshTaskResult dispatchAssetAllocations(List<String> fundCodes, String trigger) {
            return null;
        }

        @Override
        public FundRefreshTaskResult handleCallback(String taskType, FundSliceRefreshCallbackCommand command) {
            return null;
        }

        @Override
        public int closeTimedOutCallbacks(int timeoutMinutes) {
            return 0;
        }

        @Override
        public int warnSlowCatalogCallbacks(int warningMinutes) {
            return 0;
        }
    }
}
