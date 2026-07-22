package com.echoamoy.holdlens.server.infrastructure.gateway;

import com.echoamoy.holdlens.server.infrastructure.gateway.dto.MarketDetailDispatchRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class MarketDetailAgentGateway {

    private static final int REQUEST_TIMEOUT_MILLIS = 10_000;

    private final RestTemplate restTemplate;

    public MarketDetailAgentGateway() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(REQUEST_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(REQUEST_TIMEOUT_MILLIS);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Value("${holdlens.agent.market-detail-data-refresh-url}")
    private String refreshUrl;

    @Value("${holdlens.agent.request-token:}")
    private String requestToken;

    public ResponseEntity<Map> dispatch(MarketDetailDispatchRequestDTO requestDTO) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (requestToken != null && !requestToken.isBlank()) {
            headers.set("X-HoldLens-Agent-Token", requestToken);
        }
        return restTemplate.postForEntity(refreshUrl, new HttpEntity<>(requestDTO, headers), Map.class);
    }
}
