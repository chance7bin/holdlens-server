package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.request.FundSliceRefreshCallbackRequest;
import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.FundSliceRefreshCallbackCommand;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class AgentFundSliceRefreshControllerTest {
    @Test
    public void catalogCallbackReturnsAcceptedWhileOtherCallbacksKeepDefaultStatus() throws Exception {
        Method catalog = AgentFundSliceRefreshController.class.getMethod(
                "catalogCallback", String.class, FundSliceRefreshCallbackRequest.class);
        Method purchase = AgentFundSliceRefreshController.class.getMethod(
                "purchaseStatusCallback", String.class, FundSliceRefreshCallbackRequest.class);

        Assert.assertEquals(HttpStatus.ACCEPTED, catalog.getAnnotation(ResponseStatus.class).value());
        Assert.assertNull(purchase.getAnnotation(ResponseStatus.class));
    }

    @Test
    public void eachEndpointPassesItsOwnTaskTypeAndRejectsUnauthorizedHeader() throws Exception {
        AgentFundSliceRefreshController controller = new AgentFundSliceRefreshController();
        FakeCase fake = new FakeCase();
        set(controller, "fundSliceRefreshCase", fake);
        set(controller, "callbackHeaderValue", "internal");
        FundSliceRefreshCallbackRequest request = new FundSliceRefreshCallbackRequest();
        request.setServerTaskId("task-1");

        try {
            controller.catalogCallback("wrong", request);
            Assert.fail("unauthorized callback must use a non-2xx exception path");
        } catch (AgentCallbackHttpException expected) {
            Assert.assertEquals(401, expected.getHttpStatus().value());
        }
        Assert.assertNull(fake.lastTaskType);
        controller.topHoldingCallback("internal", request);
        Assert.assertEquals(ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, fake.lastTaskType);
        FundSliceRefreshCallbackRequest.FundItem item = new FundSliceRefreshCallbackRequest.FundItem();
        item.setFundCode("000001");
        item.setAssetAllocationAsOf("2026-06-30");
        item.setAllocationStatus("available");
        FundSliceRefreshCallbackRequest.AssetAllocation allocation =
                new FundSliceRefreshCallbackRequest.AssetAllocation();
        allocation.setAssetType("stock");
        allocation.setAssetTypeName("股票");
        allocation.setAllocationRatio(new java.math.BigDecimal("70.0000"));
        allocation.setDisplayOrder(1);
        item.setAssetAllocations(List.of(allocation));
        request.setFunds(List.of(item));
        controller.assetAllocationCallback("internal", request);
        Assert.assertEquals(ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH, fake.lastTaskType);
        Assert.assertEquals("股票", fake.lastCommand.getFunds().get(0)
                .getAssetAllocations().get(0).getAssetTypeName());

        fake.fail = true;
        try {
            controller.catalogCallback("internal", request);
            Assert.fail("callback transaction failure must use a non-2xx exception path");
        } catch (AgentCallbackHttpException expected) {
            Assert.assertEquals(500, expected.getHttpStatus().value());
        }
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeCase implements IFundSliceRefreshCase {
        String lastTaskType;
        FundSliceRefreshCallbackCommand lastCommand;
        boolean fail;
        public FundRefreshTaskResult scheduleCatalog(String trigger) { return null; }
        public FundRefreshTaskResult schedulePurchaseStatus(String trigger) { return null; }
        public FundRefreshTaskResult schedulePeriodReturn(String trigger) { return null; }
        public List<FundRefreshTaskResult> scheduleTopHoldings(String trigger, int batchSize) { return List.of(); }
        public List<FundRefreshTaskResult> scheduleAssetAllocations(String trigger, int batchSize) { return List.of(); }
        public FundRefreshTaskResult dispatchTopHoldings(List<String> fundCodes, String trigger) { return null; }
        public FundRefreshTaskResult dispatchAssetAllocations(List<String> fundCodes, String trigger) { return null; }
        public FundRefreshTaskResult handleCallback(String taskType, FundSliceRefreshCallbackCommand command) {
            if (fail) throw new RuntimeException("database unavailable");
            lastTaskType = taskType;
            lastCommand = command;
            return FundRefreshTaskResult.builder().serverTaskId("task-1").taskType(taskType).status("succeeded").build();
        }
        public int closeTimedOutCallbacks(int timeoutMinutes) { return 0; }
    }
}
