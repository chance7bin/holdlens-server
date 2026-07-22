package com.echoamoy.holdlens.server.infrastructure.adapter.port;

import com.echoamoy.holdlens.server.domain.marketdetail.adapter.port.IAgentMarketDetailRefreshPort;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.MarketDetailDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.valobj.MarketDetailDispatchResultVO;
import com.echoamoy.holdlens.server.infrastructure.gateway.MarketDetailAgentGateway;
import com.echoamoy.holdlens.server.infrastructure.gateway.dto.MarketDetailDispatchRequestDTO;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentMarketDetailRefreshPort implements IAgentMarketDetailRefreshPort {

    @Resource private MarketDetailAgentGateway gateway;

    @Override
    public MarketDetailDispatchResultVO dispatch(MarketDetailDispatchCommandEntity command) {
        ResponseEntity<Map> response = gateway.dispatch(MarketDetailDispatchRequestDTO.builder()
                .schemaVersion(command.getSchemaVersion()).serverTaskId(command.getServerTaskId())
                .assetKind(command.getAssetKind()).assetRef(command.getAssetRef())
                .providerMarketCode(command.getProviderMarketCode()).slices(command.getSlices()).periods(command.getPeriods())
                .callbackUrl(command.getCallbackUrl()).allowNetwork(command.getAllowNetwork())
                .requestedAt(command.getRequestedAt()).build());
        Object returnedTaskId = response.getBody() == null ? null : response.getBody().get("server_task_id");
        boolean accepted = response.getStatusCode().is2xxSuccessful()
                && command.getServerTaskId().equals(returnedTaskId == null ? null : String.valueOf(returnedTaskId));
        return new MarketDetailDispatchResultVO(accepted,
                accepted ? null : "agent rejected market detail refresh task");
    }
}
