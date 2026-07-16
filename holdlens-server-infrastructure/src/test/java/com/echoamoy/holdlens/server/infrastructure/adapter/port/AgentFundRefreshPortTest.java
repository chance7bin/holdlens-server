package com.echoamoy.holdlens.server.infrastructure.adapter.port;

import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Map;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundSliceRefreshDispatchCommandEntity;
import java.util.List;

public class AgentFundRefreshPortTest {

    @Test
    public void dispatchResultAcceptsAgentAcceptedStatus() throws Exception {
        AgentFundRefreshPort port = new AgentFundRefreshPort();
        Method method = AgentFundRefreshPort.class.getDeclaredMethod("toDispatchResult", ResponseEntity.class);
        method.setAccessible(true);

        FundRefreshDispatchResultEntity result = (FundRefreshDispatchResultEntity) method.invoke(port,
                ResponseEntity.accepted().body(Map.of("status", "accepted")));

        Assert.assertTrue(result.isAccepted());
        Assert.assertEquals("accepted", result.getAgentStatus());
    }

    @Test
    public void fundSliceTaskTypesRouteToFrozenEndpoints() throws Exception {
        AgentFundRefreshPort port = new AgentFundRefreshPort();
        set(port, "fundCatalogRefreshUrl", "/tasks/fund-catalog-refresh");
        set(port, "fundPurchaseStatusRefreshUrl", "/tasks/fund-purchase-status-refresh");
        set(port, "fundPeriodReturnRefreshUrl", "/tasks/fund-period-return-refresh");
        set(port, "fundTopHoldingRefreshUrl", "/tasks/fund-top-holding-refresh");
        set(port, "fundAssetAllocationRefreshUrl", "/tasks/fund-asset-allocation-refresh");
        Method route = AgentFundRefreshPort.class.getDeclaredMethod("sliceUrl", String.class);
        route.setAccessible(true);
        Assert.assertEquals("/tasks/fund-catalog-refresh", route.invoke(port, ProcessingTaskEntity.FUND_CATALOG_REFRESH));
        Assert.assertEquals("/tasks/fund-purchase-status-refresh", route.invoke(port, ProcessingTaskEntity.FUND_PURCHASE_STATUS_REFRESH));
        Assert.assertEquals("/tasks/fund-period-return-refresh", route.invoke(port, ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH));
        Assert.assertEquals("/tasks/fund-top-holding-refresh", route.invoke(port, ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH));
        Assert.assertEquals("/tasks/fund-asset-allocation-refresh",
                route.invoke(port, ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH));
    }

    @Test
    public void assetAllocationPayloadContainsFundCodesButNoReportPeriod() throws Exception {
        AgentFundRefreshPort port = new AgentFundRefreshPort();
        Method builder = AgentFundRefreshPort.class.getDeclaredMethod(
                "buildSliceRequest", FundSliceRefreshDispatchCommandEntity.class);
        builder.setAccessible(true);
        FundSliceRefreshDispatchCommandEntity command = FundSliceRefreshDispatchCommandEntity.builder()
                .taskType(ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH)
                .schemaVersion("fund-asset-allocation-refresh-task/v1")
                .serverTaskId("fund_asset_allocation_refresh_1")
                .fundCodes(List.of("000001", "000002"))
                .allowNetwork(true)
                .callbackUrl("http://server/internal/agent/fund-asset-allocation-refresh/callback")
                .build();

        Map<?, ?> payload = (Map<?, ?>) builder.invoke(port, command);

        Assert.assertEquals(command.getSchemaVersion(), payload.get("schema_version"));
        Assert.assertEquals(command.getFundCodes(), payload.get("fund_codes"));
        Assert.assertEquals(command.getCallbackUrl(), payload.get("callback_url"));
        Assert.assertFalse(payload.containsKey("asset_allocation_as_of"));
        Assert.assertFalse(payload.containsKey("report_period"));
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

}
