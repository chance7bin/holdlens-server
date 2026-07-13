package com.echoamoy.holdlens.server.infrastructure.adapter.port;

import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.Map;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;

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
        Method route = AgentFundRefreshPort.class.getDeclaredMethod("sliceUrl", String.class);
        route.setAccessible(true);
        Assert.assertEquals("/tasks/fund-catalog-refresh", route.invoke(port, ProcessingTaskEntity.FUND_CATALOG_REFRESH));
        Assert.assertEquals("/tasks/fund-purchase-status-refresh", route.invoke(port, ProcessingTaskEntity.FUND_PURCHASE_STATUS_REFRESH));
        Assert.assertEquals("/tasks/fund-period-return-refresh", route.invoke(port, ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH));
        Assert.assertEquals("/tasks/fund-top-holding-refresh", route.invoke(port, ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH));
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

}
