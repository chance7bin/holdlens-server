package com.echoamoy.holdlens.server.infrastructure.adapter.port;

import com.echoamoy.holdlens.server.domain.processing.model.entity.StockQuoteRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteTargetEntity;
import com.sun.net.httpserver.HttpServer;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AgentFundRefreshPortTest {

    @Test
    public void dispatchStockQuoteRefreshUsesAgentSnakeCasePayload() throws Exception {
        AgentFundRefreshPort port = new AgentFundRefreshPort();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/tasks/stock-quote-refresh", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"server_task_id\":\"server_stock_task_1\",\"status\":\"accepted\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(202, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            setField(port, "stockRefreshUrl",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/tasks/stock-quote-refresh");

            port.dispatch(StockQuoteRefreshDispatchCommandEntity.builder()
                    .schemaVersion("stock-quote-refresh-task/v1")
                    .serverTaskId("server_stock_task_1")
                    .stocks(List.of(StockQuoteTargetEntity.builder()
                            .stockCode("600000")
                            .market("1")
                            .build()))
                    .allowNetwork(Boolean.TRUE)
                    .callbackUrl("http://127.0.0.1:8091/internal/agent/stock-quote-refresh/callback")
                    .build());
        } finally {
            server.stop(0);
        }

        Assert.assertTrue(requestBody.get().contains("\"stock_code\":\"600000\""));
        Assert.assertTrue(requestBody.get().contains("\"market\":\"1\""));
        Assert.assertFalse(requestBody.get().contains("\"stockCode\""));
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

}
