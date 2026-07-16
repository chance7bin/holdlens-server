package com.echoamoy.holdlens.server.infrastructure.adapter.port;

import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentAShareMarketRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentFundSliceRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentUSStockMarketRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.model.entity.AShareMarketRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundSliceRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.USStockMarketRefreshDispatchCommandEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class AgentFundRefreshPort implements IAgentFundSliceRefreshPort, IAgentAShareMarketRefreshPort, IAgentUSStockMarketRefreshPort {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${holdlens.agent.fund-catalog-refresh-url}")
    private String fundCatalogRefreshUrl;

    @Value("${holdlens.agent.fund-purchase-status-refresh-url}")
    private String fundPurchaseStatusRefreshUrl;

    @Value("${holdlens.agent.fund-period-return-refresh-url}")
    private String fundPeriodReturnRefreshUrl;

    @Value("${holdlens.agent.fund-top-holding-refresh-url}")
    private String fundTopHoldingRefreshUrl;

    @Value("${holdlens.agent.a-share-market-refresh-url}")
    private String aShareMarketRefreshUrl;

    @Value("${holdlens.agent.us-stock-market-refresh-url}")
    private String usStockMarketRefreshUrl;

    @Override
    public FundRefreshDispatchResultEntity dispatch(FundSliceRefreshDispatchCommandEntity commandEntity) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("schema_version", commandEntity.getSchemaVersion());
        request.put("server_task_id", commandEntity.getServerTaskId());
        if (ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH.equals(commandEntity.getTaskType())) {
            request.put("fund_codes", commandEntity.getFundCodes());
        }
        request.put("allow_network", commandEntity.getAllowNetwork());
        request.put("callback_url", commandEntity.getCallbackUrl());
        ResponseEntity<Map> response = restTemplate.postForEntity(sliceUrl(commandEntity.getTaskType()), request, Map.class);
        return toDispatchResult(response);
    }

    private String sliceUrl(String taskType) {
        return switch (taskType) {
            case ProcessingTaskEntity.FUND_CATALOG_REFRESH -> fundCatalogRefreshUrl;
            case ProcessingTaskEntity.FUND_PURCHASE_STATUS_REFRESH -> fundPurchaseStatusRefreshUrl;
            case ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH -> fundPeriodReturnRefreshUrl;
            case ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH -> fundTopHoldingRefreshUrl;
            default -> throw new IllegalArgumentException("unsupported fund slice task type: " + taskType);
        };
    }

    @Override
    public FundRefreshDispatchResultEntity dispatch(AShareMarketRefreshDispatchCommandEntity commandEntity) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("schema_version", commandEntity.getSchemaVersion());
        request.put("server_task_id", commandEntity.getServerTaskId());
        request.put("allow_network", commandEntity.getAllowNetwork());
        request.put("callback_url", commandEntity.getCallbackUrl());

        ResponseEntity<Map> response = restTemplate.postForEntity(aShareMarketRefreshUrl, request, Map.class);
        return toDispatchResult(response);
    }

    @Override
    public FundRefreshDispatchResultEntity dispatch(USStockMarketRefreshDispatchCommandEntity commandEntity) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("schema_version", commandEntity.getSchemaVersion());
        request.put("server_task_id", commandEntity.getServerTaskId());
        request.put("allow_network", commandEntity.getAllowNetwork());
        request.put("callback_url", commandEntity.getCallbackUrl());

        ResponseEntity<Map> response = restTemplate.postForEntity(usStockMarketRefreshUrl, request, Map.class);
        return toDispatchResult(response);
    }

    private FundRefreshDispatchResultEntity toDispatchResult(ResponseEntity<Map> response) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return FundRefreshDispatchResultEntity.builder()
                    .accepted(false)
                    .errorSummary("agent response status " + response.getStatusCode().value())
                    .build();
        }

        Map<?, ?> body = response.getBody();
        String status = stringValue(body.get("status"));
        boolean accepted = "accepted".equals(status) || "running".equals(status);
        return FundRefreshDispatchResultEntity.builder()
                .accepted(accepted)
                .agentStatus(status)
                .errorSummary(stringValue(body.get("error_summary")))
                .build();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

}
