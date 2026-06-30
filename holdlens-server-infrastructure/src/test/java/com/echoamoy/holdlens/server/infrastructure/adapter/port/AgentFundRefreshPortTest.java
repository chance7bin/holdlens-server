package com.echoamoy.holdlens.server.infrastructure.adapter.port;

import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.Map;

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

}
