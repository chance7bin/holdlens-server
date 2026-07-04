package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.dto.FundRefreshTaskDTO;
import com.echoamoy.holdlens.server.api.request.AShareMarketRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.AShareMarketRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.request.USStockMarketRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.USStockMarketRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AgentFundRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

public class AgentFundRefreshControllerTest {

    @Test
    public void createAShareMarketTaskDelegatesToCase() throws Exception {
        FakeAgentFundRefreshCase refreshCase = new FakeAgentFundRefreshCase();
        AgentFundRefreshController controller = newController(refreshCase);

        Response<FundRefreshTaskDTO> response = controller.createAShareMarketTask(AShareMarketRefreshCreateRequest.builder()
                .trigger("manual")
                .build());

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals(ProcessingTaskEntity.A_SHARE_MARKET_REFRESH, response.getData().getTaskType());
        Assert.assertEquals("manual", refreshCase.lastCreateCommand.getTrigger());
    }

    @Test
    public void aShareMarketCallbackMapsFullMarketPayload() throws Exception {
        FakeAgentFundRefreshCase refreshCase = new FakeAgentFundRefreshCase();
        AgentFundRefreshController controller = newController(refreshCase);

        Response<FundRefreshTaskDTO> response = controller.aShareMarketCallback("internal", AShareMarketRefreshCallbackRequest.builder()
                .schemaVersion("a-share-market-refresh-result/v1")
                .serverTaskId("a_share_market_refresh_1")
                .idempotencyKey("a_share_market_refresh_1:result:1")
                .status("succeeded")
                .generatedAt("2026-06-18T10:00:00+08:00")
                .market("A_SHARE")
                .stocks(List.of(AShareMarketRefreshCallbackRequest.StockMarket.builder()
                        .stockCode("600000")
                        .stockName("测试股份")
                        .market("A_SHARE")
                        .status("active")
                        .currency("CNY")
                        .volumeUnit("LOT")
                        .latestPrice("10.23")
                        .changePercent("1.25")
                        .volume("1234567")
                        .refreshedAt("2026-06-18T10:00:00+08:00")
                        .build()))
                .build());

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals("a_share_market_refresh_1", refreshCase.lastCallbackCommand.getServerTaskId());
        Assert.assertEquals("10.23", refreshCase.lastCallbackCommand.getStocks().get(0).getLatestPrice());
        Assert.assertEquals("1.25", refreshCase.lastCallbackCommand.getStocks().get(0).getChangePercent());
        Assert.assertEquals("1234567", refreshCase.lastCallbackCommand.getStocks().get(0).getVolume());
        Assert.assertEquals("CNY", refreshCase.lastCallbackCommand.getStocks().get(0).getCurrency());
        Assert.assertEquals("LOT", refreshCase.lastCallbackCommand.getStocks().get(0).getVolumeUnit());
        Assert.assertEquals("active", refreshCase.lastCallbackCommand.getStocks().get(0).getStatus());
    }

    @Test
    public void aShareMarketCallbackRejectsUnauthorizedHeader() throws Exception {
        AgentFundRefreshController controller = newController(new FakeAgentFundRefreshCase());

        Response<FundRefreshTaskDTO> response = controller.aShareMarketCallback("bad", AShareMarketRefreshCallbackRequest.builder().build());

        Assert.assertEquals("0002", response.getCode());
    }

    @Test
    public void createUSStockMarketTaskDelegatesToCase() throws Exception {
        FakeAgentFundRefreshCase refreshCase = new FakeAgentFundRefreshCase();
        AgentFundRefreshController controller = newController(refreshCase);

        Response<FundRefreshTaskDTO> response = controller.createUSStockMarketTask(USStockMarketRefreshCreateRequest.builder()
                .trigger("manual")
                .build());

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals(ProcessingTaskEntity.US_STOCK_MARKET_REFRESH, response.getData().getTaskType());
        Assert.assertEquals("manual", refreshCase.lastUSCreateCommand.getTrigger());
    }

    @Test
    public void usStockMarketCallbackMapsUSFields() throws Exception {
        FakeAgentFundRefreshCase refreshCase = new FakeAgentFundRefreshCase();
        AgentFundRefreshController controller = newController(refreshCase);

        Response<FundRefreshTaskDTO> response = controller.usStockMarketCallback("internal", USStockMarketRefreshCallbackRequest.builder()
                .schemaVersion("us-stock-market-refresh-result/v1")
                .serverTaskId("us_stock_market_refresh_1")
                .idempotencyKey("us_stock_market_refresh_1:result:1")
                .status("succeeded")
                .generatedAt("2026-07-04T10:00:00+08:00")
                .market("US_STOCK")
                .stocks(List.of(USStockMarketRefreshCallbackRequest.StockMarket.builder()
                        .stockCode("NVDA")
                        .stockName("NVIDIA")
                        .market("US_STOCK")
                        .providerMarketCode("105")
                        .currency("USD")
                        .volumeUnit("SHARE")
                        .latestPrice("172.41")
                        .peRatio("56.789")
                        .listingDate("1999-01-22")
                        .refreshedAt("2026-07-04T10:00:00+08:00")
                        .build()))
                .build());

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals("us_stock_market_refresh_1", refreshCase.lastUSCallbackCommand.getServerTaskId());
        Assert.assertEquals("172.41", refreshCase.lastUSCallbackCommand.getStocks().get(0).getLatestPrice());
        Assert.assertEquals("USD", refreshCase.lastUSCallbackCommand.getStocks().get(0).getCurrency());
        Assert.assertEquals("SHARE", refreshCase.lastUSCallbackCommand.getStocks().get(0).getVolumeUnit());
        Assert.assertEquals("56.789", refreshCase.lastUSCallbackCommand.getStocks().get(0).getPeRatio());
        Assert.assertEquals("1999-01-22", refreshCase.lastUSCallbackCommand.getStocks().get(0).getListingDate());
    }

    private AgentFundRefreshController newController(FakeAgentFundRefreshCase refreshCase) throws Exception {
        AgentFundRefreshController controller = new AgentFundRefreshController();
        setField(controller, "agentFundRefreshCase", refreshCase);
        setField(controller, "callbackHeaderValue", "internal");
        return controller;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeAgentFundRefreshCase implements IAgentFundRefreshCase {
        private AShareMarketRefreshCreateCommand lastCreateCommand;
        private AShareMarketRefreshCallbackCommand lastCallbackCommand;
        private USStockMarketRefreshCreateCommand lastUSCreateCommand;
        private USStockMarketRefreshCallbackCommand lastUSCallbackCommand;

        @Override
        public FundRefreshTaskResult createAndDispatch(FundRefreshCreateCommand command) {
            return null;
        }

        @Override
        public FundRefreshTaskResult handleCallback(AgentFundRefreshCallbackCommand command) {
            return null;
        }

        @Override
        public FundRefreshTaskResult queryTask(String serverTaskId) {
            return null;
        }

        @Override
        public FundRefreshTaskResult createAndDispatchAShareMarket(AShareMarketRefreshCreateCommand command) {
            lastCreateCommand = command;
            return FundRefreshTaskResult.builder()
                    .serverTaskId("a_share_market_refresh_1")
                    .taskType(ProcessingTaskEntity.A_SHARE_MARKET_REFRESH)
                    .status("dispatched")
                    .build();
        }

        @Override
        public boolean hasNonTerminalTask(String taskType) {
            return false;
        }

        @Override
        public FundRefreshTaskResult handleAShareMarketCallback(AShareMarketRefreshCallbackCommand command) {
            lastCallbackCommand = command;
            return FundRefreshTaskResult.builder()
                    .serverTaskId(command.getServerTaskId())
                    .taskType(ProcessingTaskEntity.A_SHARE_MARKET_REFRESH)
                    .status(command.getStatus())
                    .build();
        }

        @Override
        public FundRefreshTaskResult createAndDispatchUSStockMarket(USStockMarketRefreshCreateCommand command) {
            lastUSCreateCommand = command;
            return FundRefreshTaskResult.builder()
                    .serverTaskId("us_stock_market_refresh_1")
                    .taskType(ProcessingTaskEntity.US_STOCK_MARKET_REFRESH)
                    .status("dispatched")
                    .build();
        }

        @Override
        public FundRefreshTaskResult handleUSStockMarketCallback(USStockMarketRefreshCallbackCommand command) {
            lastUSCallbackCommand = command;
            return FundRefreshTaskResult.builder()
                    .serverTaskId(command.getServerTaskId())
                    .taskType(ProcessingTaskEntity.US_STOCK_MARKET_REFRESH)
                    .status(command.getStatus())
                    .build();
        }
    }
}
