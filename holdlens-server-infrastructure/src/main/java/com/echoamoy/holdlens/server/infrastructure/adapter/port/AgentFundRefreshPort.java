package com.echoamoy.holdlens.server.infrastructure.adapter.port;

import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentFundRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentStockQuoteRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.StockQuoteRefreshDispatchCommandEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class AgentFundRefreshPort implements IAgentFundRefreshPort, IAgentStockQuoteRefreshPort {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${holdlens.agent.fund-refresh-url:http://127.0.0.1:5677/tasks/fund-detail-refresh}")
    private String fundRefreshUrl;

    @Value("${holdlens.agent.stock-refresh-url:http://127.0.0.1:5677/tasks/stock-quote-refresh}")
    private String stockRefreshUrl;

    @Override
    public FundRefreshDispatchResultEntity dispatch(FundRefreshDispatchCommandEntity commandEntity) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("schema_version", commandEntity.getSchemaVersion());
        request.put("server_task_id", commandEntity.getServerTaskId());
        request.put("fund_codes", commandEntity.getFundCodes());
        request.put("allow_network", commandEntity.getAllowNetwork());
        request.put("callback_url", commandEntity.getCallbackUrl());

        ResponseEntity<Map> response = restTemplate.postForEntity(fundRefreshUrl, request, Map.class);
        return toDispatchResult(response);
    }

    @Override
    public FundRefreshDispatchResultEntity dispatch(StockQuoteRefreshDispatchCommandEntity commandEntity) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("schema_version", commandEntity.getSchemaVersion());
        request.put("server_task_id", commandEntity.getServerTaskId());
        request.put("stocks", commandEntity.getStocks());
        request.put("allow_network", commandEntity.getAllowNetwork());
        request.put("callback_url", commandEntity.getCallbackUrl());

        ResponseEntity<Map> response = restTemplate.postForEntity(stockRefreshUrl, request, Map.class);
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
