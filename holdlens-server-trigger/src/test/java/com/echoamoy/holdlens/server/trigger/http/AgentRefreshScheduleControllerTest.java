package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.FundSliceRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.marketdetail.IMarketDataRefreshScheduleCase;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.List;

public class AgentRefreshScheduleControllerTest {

    @Test
    public void testRunFundCatalogRefreshSchedule() {
        RecordingFundSliceRefreshCase refreshCase = new RecordingFundSliceRefreshCase();
        AgentRefreshScheduleController controller = newController(refreshCase, new RecordingMarketDataRefreshScheduleCase());

        Response<Void> response = controller.runFundCatalogRefreshSchedule();

        assertSuccess(response);
        Assert.assertEquals(1, refreshCase.fundCatalogRunCount);
        Assert.assertEquals("manual", refreshCase.fundCatalogTrigger);
    }

    @Test
    public void testRunFundPurchaseStatusRefreshSchedule() throws Exception {
        RecordingFundSliceRefreshCase refreshCase = new RecordingFundSliceRefreshCase();
        AgentRefreshScheduleController controller = newController(refreshCase, new RecordingMarketDataRefreshScheduleCase());

        assertSuccess(controller.runFundPurchaseStatusRefreshSchedule());

        Assert.assertEquals(1, refreshCase.fundPurchaseStatusRunCount);
        Assert.assertEquals("manual", refreshCase.fundPurchaseStatusTrigger);
        assertMapping("runFundPurchaseStatusRefreshSchedule",
                "/api/agent/fund-purchase-status-refresh/schedule-runs");
    }

    @Test
    public void testFundCatalogRefreshScheduleMapping() throws Exception {
        assertMapping("runFundCatalogRefreshSchedule", "/api/agent/fund-catalog-refresh/schedule-runs");
    }

    @Test
    public void testRunFundTopHoldingRefreshSchedule() {
        RecordingFundSliceRefreshCase refreshCase = new RecordingFundSliceRefreshCase();
        AgentRefreshScheduleController controller = newController(refreshCase, new RecordingMarketDataRefreshScheduleCase());

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
        AgentRefreshScheduleController controller = newController(refreshCase, new RecordingMarketDataRefreshScheduleCase());

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

    @Test
    public void testRunFundSliceCallbackTimeoutSchedule() throws Exception {
        RecordingFundSliceRefreshCase refreshCase = new RecordingFundSliceRefreshCase();
        AgentRefreshScheduleController controller = newController(refreshCase, new RecordingMarketDataRefreshScheduleCase());

        assertSuccess(controller.runFundSliceCallbackTimeoutSchedule());

        Assert.assertEquals(1, refreshCase.closeTimedOutCallbacksRunCount);
        Assert.assertEquals(60, refreshCase.callbackTimeoutMinutes);
        Assert.assertEquals(1, refreshCase.warnSlowCatalogCallbacksRunCount);
        Assert.assertEquals(10, refreshCase.callbackProcessingWarningMinutes);
        assertMapping("runFundSliceCallbackTimeoutSchedule",
                "/api/agent/fund-slice-callback-timeout/schedule-runs");
    }

    @Test
    public void testRunMarketDataRefreshSchedules() throws Exception {
        RecordingMarketDataRefreshScheduleCase refreshCase = new RecordingMarketDataRefreshScheduleCase();
        AgentRefreshScheduleController controller = newController(new RecordingFundSliceRefreshCase(), refreshCase);

        assertSuccess(controller.runAShareMarketRefreshSchedule());
        assertSuccess(controller.runUSStockMarketRefreshSchedule());
        assertSuccess(controller.runActiveFundDetailRefreshSchedule());
        assertSuccess(controller.runActiveAShareDetailRefreshSchedule());
        assertSuccess(controller.runActiveUSStockDetailRefreshSchedule());

        Assert.assertEquals(1, refreshCase.aShareMarketRunCount);
        Assert.assertEquals(1, refreshCase.usStockMarketRunCount);
        Assert.assertEquals(1, refreshCase.fundDetailRunCount);
        Assert.assertEquals(List.of(
                StockMarketEntity.MARKET_A_SHARE,
                StockMarketEntity.MARKET_US_STOCK), refreshCase.stockDetailMarkets);
        assertMapping("runAShareMarketRefreshSchedule", "/api/agent/a-share-market-refresh/schedule-runs");
        assertMapping("runUSStockMarketRefreshSchedule", "/api/agent/us-stock-market-refresh/schedule-runs");
        assertMapping("runActiveFundDetailRefreshSchedule", "/api/agent/active-fund-detail-refresh/schedule-runs");
        assertMapping("runActiveAShareDetailRefreshSchedule",
                "/api/agent/active-a-share-detail-refresh/schedule-runs");
        assertMapping("runActiveUSStockDetailRefreshSchedule",
                "/api/agent/active-us-stock-detail-refresh/schedule-runs");
    }

    private AgentRefreshScheduleController newController(
            RecordingFundSliceRefreshCase fundRefreshCase,
            RecordingMarketDataRefreshScheduleCase marketRefreshCase) {
        return new AgentRefreshScheduleController(fundRefreshCase, marketRefreshCase, 20, 30, 60, 10);
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
        private int fundPurchaseStatusRunCount;
        private String fundPurchaseStatusTrigger;
        private int fundTopHoldingRunCount;
        private String fundTopHoldingTrigger;
        private int fundTopHoldingBatchSize;
        private int fundAssetAllocationRunCount;
        private String fundAssetAllocationTrigger;
        private int fundAssetAllocationBatchSize;
        private int closeTimedOutCallbacksRunCount;
        private int callbackTimeoutMinutes;
        private int warnSlowCatalogCallbacksRunCount;
        private int callbackProcessingWarningMinutes;

        @Override
        public FundRefreshTaskResult scheduleCatalog(String trigger) {
            fundCatalogRunCount++;
            fundCatalogTrigger = trigger;
            return null;
        }

        @Override
        public FundRefreshTaskResult schedulePurchaseStatus(String trigger) {
            fundPurchaseStatusRunCount++;
            fundPurchaseStatusTrigger = trigger;
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
            closeTimedOutCallbacksRunCount++;
            callbackTimeoutMinutes = timeoutMinutes;
            return 0;
        }

        @Override
        public int warnSlowCatalogCallbacks(int warningMinutes) {
            warnSlowCatalogCallbacksRunCount++;
            callbackProcessingWarningMinutes = warningMinutes;
            return 0;
        }
    }

    private static class RecordingMarketDataRefreshScheduleCase implements IMarketDataRefreshScheduleCase {
        private int aShareMarketRunCount;
        private int usStockMarketRunCount;
        private int fundDetailRunCount;
        private final List<String> stockDetailMarkets = new java.util.ArrayList<>();

        @Override
        public boolean runAShareMarketRefresh() {
            aShareMarketRunCount++;
            return true;
        }

        @Override
        public boolean runUSStockMarketRefresh() {
            usStockMarketRunCount++;
            return true;
        }

        @Override
        public int runFundDetailRefresh() {
            fundDetailRunCount++;
            return 1;
        }

        @Override
        public int runStockDetailRefresh(String market) {
            stockDetailMarkets.add(market);
            return 1;
        }
    }
}
