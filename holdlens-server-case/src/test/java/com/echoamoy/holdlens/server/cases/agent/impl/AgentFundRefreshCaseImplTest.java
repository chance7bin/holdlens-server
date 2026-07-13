package com.echoamoy.holdlens.server.cases.agent.impl;

import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.support.TransactionExecutor;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentAShareMarketRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentUSStockMarketRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.repository.IProcessingTaskRepository;
import com.echoamoy.holdlens.server.domain.processing.model.entity.AShareMarketRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingLogEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.USStockMarketRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.valobj.ProcessingTaskStatusEnumVO;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import com.echoamoy.holdlens.server.types.exception.AppException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class AgentFundRefreshCaseImplTest {

    @Test
    public void createAShareMarketTaskDispatchesFullMarketRequest() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeAgentPort agentPort = new FakeAgentPort(true, "accepted");
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort);

        FundRefreshTaskResult result = refreshCase.createAndDispatchAShareMarket(AShareMarketRefreshCreateCommand.builder()
                .trigger("manual")
                .build());

        Assert.assertEquals("dispatched", result.getStatus());
        Assert.assertEquals(ProcessingTaskEntity.A_SHARE_MARKET_REFRESH, result.getTaskType());
        Assert.assertEquals("a-share-market-refresh-task/v1", agentPort.lastAShareCommand.getSchemaVersion());
        Assert.assertEquals(Boolean.TRUE, agentPort.lastAShareCommand.getAllowNetwork());
        Assert.assertEquals("http://server/internal/agent/a-share-market-refresh/callback", agentPort.lastAShareCommand.getCallbackUrl());
        Assert.assertTrue(processingRepository.queryTask(result.getServerTaskId()).getTaskParamsJson().contains("\"market\":\"A_SHARE\""));
    }

    @Test
    public void createAShareMarketTaskRejectsConcurrentNonTerminalTask() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("a_share_market_refresh_running")
                .taskType(ProcessingTaskEntity.A_SHARE_MARKET_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .build());
        FakeAgentPort agentPort = new FakeAgentPort(true, "running");
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort);

        try {
            refreshCase.createAndDispatchAShareMarket(AShareMarketRefreshCreateCommand.builder().build());
            Assert.fail("should reject concurrent task");
        } catch (AppException e) {
            Assert.assertTrue(e.getInfo().contains("正在运行"));
            Assert.assertEquals(e.getInfo(), e.getMessage());
            Assert.assertNull(agentPort.lastAShareCommand);
        }
    }

    @Test
    public void handleAShareMarketCallbackPersistsStocksWarningsAndNumericDiagnosticsOnce() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeStockMarketRepository stockMarketRepository = new FakeStockMarketRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"),
                stockMarketRepository);
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("a_share_task_1")
                .taskType(ProcessingTaskEntity.A_SHARE_MARKET_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .build());

        AShareMarketRefreshCallbackCommand callback = AShareMarketRefreshCallbackCommand.builder()
                .schemaVersion("a-share-market-refresh-result/v1")
                .serverTaskId("a_share_task_1")
                .idempotencyKey("a_share_task_1:result:1")
                .status("partial_failed")
                .generatedAt("2026-06-18T02:05:30Z")
                .market(StockMarketEntity.MARKET_A_SHARE)
                .stocks(List.of(AShareMarketRefreshCallbackCommand.StockMarket.builder()
                        .stockCode("600000")
                        .stockName("测试股份")
                        .market(StockMarketEntity.MARKET_A_SHARE)
                        .exchangeCode("SH")
                        .providerMarketCode("1")
                        .latestPrice("10.23")
                        .changePercent("1.25")
                        .volume("1234567")
                        .turnoverAmount("bad-number")
                        .refreshedAt("2026-06-18T10:04:30+08:00")
                        .build()))
                .refreshWarnings(List.of(AShareMarketRefreshCallbackCommand.RefreshWarning.builder()
                        .module("a_share_market_refresh")
                        .event("provider_failed")
                        .message("provider failed for one page")
                        .severity("warning")
                        .build()))
                .build();

        FundRefreshTaskResult first = refreshCase.handleAShareMarketCallback(callback);
        FundRefreshTaskResult duplicate = refreshCase.handleAShareMarketCallback(callback);

        Assert.assertEquals("partial_failed", first.getStatus());
        Assert.assertEquals("partial_failed", duplicate.getStatus());
        Assert.assertEquals(1, stockMarketRepository.upsertCount);
        StockMarketEntity stock = stockMarketRepository.upsertedMarkets.get(0);
        Assert.assertEquals(StockMarketEntity.MARKET_A_SHARE, stock.getMarket());
        Assert.assertEquals("SH", stock.getExchangeCode());
        Assert.assertEquals(StockMarketEntity.CURRENCY_CNY, stock.getCurrency());
        Assert.assertEquals(StockMarketEntity.VOLUME_UNIT_LOT, stock.getVolumeUnit());
        Assert.assertEquals(new BigDecimal("10.23"), stock.getLatestPrice());
        Assert.assertEquals(new BigDecimal("1.25"), stock.getChangePercent());
        Assert.assertEquals(Long.valueOf(1234567L), stock.getVolume());
        Assert.assertNull(stock.getTurnoverAmount());
        Assert.assertEquals(LocalDateTime.of(2026, 6, 18, 10, 4, 30), stock.getRefreshedAt());
        Assert.assertEquals(2, processingRepository.logs.size());
        Assert.assertEquals("a_share_market_refresh", processingRepository.logs.get(0).getModule());
        Assert.assertEquals("numeric_parse_failed", processingRepository.logs.get(1).getEvent());
    }

    @Test
    public void handleAShareMarketCallbackRejectsUnsupportedSchemaAndMarksTaskFailed() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"));
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("a_share_bad_schema")
                .taskType(ProcessingTaskEntity.A_SHARE_MARKET_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .build());

        try {
            refreshCase.handleAShareMarketCallback(AShareMarketRefreshCallbackCommand.builder()
                    .schemaVersion("unknown/v1")
                    .serverTaskId("a_share_bad_schema")
                    .idempotencyKey("a_share_bad_schema:result:1")
                    .status("succeeded")
                    .build());
            Assert.fail("should reject unsupported schema");
        } catch (AppException e) {
            Assert.assertEquals("failed", processingRepository.queryTask("a_share_bad_schema").getStatus().getCode());
        }
    }

    @Test
    public void createUSStockMarketTaskDispatchesFullMarketRequest() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeAgentPort agentPort = new FakeAgentPort(true, "accepted");
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort);

        FundRefreshTaskResult result = refreshCase.createAndDispatchUSStockMarket(USStockMarketRefreshCreateCommand.builder()
                .trigger("manual")
                .build());

        Assert.assertEquals("dispatched", result.getStatus());
        Assert.assertEquals(ProcessingTaskEntity.US_STOCK_MARKET_REFRESH, result.getTaskType());
        Assert.assertEquals("us-stock-market-refresh-task/v1", agentPort.lastUSStockCommand.getSchemaVersion());
        Assert.assertEquals(Boolean.TRUE, agentPort.lastUSStockCommand.getAllowNetwork());
        Assert.assertEquals("http://server/internal/agent/us-stock-market-refresh/callback", agentPort.lastUSStockCommand.getCallbackUrl());
        Assert.assertTrue(processingRepository.queryTask(result.getServerTaskId()).getTaskParamsJson().contains("\"market\":\"US_STOCK\""));
    }

    @Test
    public void createUSStockMarketTaskRejectsConcurrentNonTerminalTask() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("us_stock_market_refresh_running")
                .taskType(ProcessingTaskEntity.US_STOCK_MARKET_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .build());
        FakeAgentPort agentPort = new FakeAgentPort(true, "running");
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort);

        try {
            refreshCase.createAndDispatchUSStockMarket(USStockMarketRefreshCreateCommand.builder().build());
            Assert.fail("should reject concurrent task");
        } catch (AppException e) {
            Assert.assertTrue(e.getInfo().contains("正在运行"));
            Assert.assertNull(agentPort.lastUSStockCommand);
        }
    }

    @Test
    public void handleUSStockMarketCallbackPersistsStocksWarningsAndUSFieldsOnce() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeStockMarketRepository stockMarketRepository = new FakeStockMarketRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"),
                stockMarketRepository);
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("us_stock_task_1")
                .taskType(ProcessingTaskEntity.US_STOCK_MARKET_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .build());

        USStockMarketRefreshCallbackCommand callback = USStockMarketRefreshCallbackCommand.builder()
                .schemaVersion("us-stock-market-refresh-result/v1")
                .serverTaskId("us_stock_task_1")
                .idempotencyKey("us_stock_task_1:result:1")
                .status("partial_failed")
                .generatedAt("2026-07-04T02:05:30Z")
                .market(StockMarketEntity.MARKET_US_STOCK)
                .stocks(List.of(USStockMarketRefreshCallbackCommand.StockMarket.builder()
                        .stockCode("NVDA")
                        .stockName("NVIDIA")
                        .market(StockMarketEntity.MARKET_US_STOCK)
                        .providerMarketCode("105")
                        .latestPrice("172.41")
                        .changePercent("1.25")
                        .volume("1234567")
                        .turnoverAmount("bad-number")
                        .peRatio("56.789")
                        .listingDate("1999-01-22")
                        .refreshedAt("2026-07-04T10:04:30+08:00")
                        .build()))
                .refreshWarnings(List.of(USStockMarketRefreshCallbackCommand.RefreshWarning.builder()
                        .module("us_stock_market_refresh")
                        .event("provider_failed")
                        .message("provider failed for one page")
                        .severity("warning")
                        .build()))
                .build();

        FundRefreshTaskResult first = refreshCase.handleUSStockMarketCallback(callback);
        FundRefreshTaskResult duplicate = refreshCase.handleUSStockMarketCallback(callback);

        Assert.assertEquals("partial_failed", first.getStatus());
        Assert.assertEquals("partial_failed", duplicate.getStatus());
        Assert.assertEquals(1, stockMarketRepository.upsertCount);
        StockMarketEntity stock = stockMarketRepository.upsertedMarkets.get(0);
        Assert.assertEquals(StockMarketEntity.MARKET_US_STOCK, stock.getMarket());
        Assert.assertEquals("105", stock.getProviderMarketCode());
        Assert.assertEquals(StockMarketEntity.CURRENCY_USD, stock.getCurrency());
        Assert.assertEquals(StockMarketEntity.VOLUME_UNIT_SHARE, stock.getVolumeUnit());
        Assert.assertEquals(new BigDecimal("172.41"), stock.getLatestPrice());
        Assert.assertEquals(new BigDecimal("56.789"), stock.getPeRatio());
        Assert.assertNull(stock.getPeDynamic());
        Assert.assertNull(stock.getTurnoverAmount());
        Assert.assertEquals(LocalDate.of(1999, 1, 22), stock.getListingDate());
        Assert.assertEquals(LocalDateTime.of(2026, 7, 4, 10, 4, 30), stock.getRefreshedAt());
        Assert.assertEquals(2, processingRepository.logs.size());
        Assert.assertEquals("us_stock_market_refresh", processingRepository.logs.get(0).getModule());
        Assert.assertEquals("us_stock_market_refresh", processingRepository.logs.get(1).getModule());
        Assert.assertEquals("numeric_parse_failed", processingRepository.logs.get(1).getEvent());
    }

    @Test
    public void handleUSStockMarketCallbackMarksCallbackFailedWhenMarketPersistenceFails() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeStockMarketRepository stockMarketRepository = new FakeStockMarketRepository();
        stockMarketRepository.failUpsert = true;
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"),
                stockMarketRepository);
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("us_stock_callback_failure")
                .taskType(ProcessingTaskEntity.US_STOCK_MARKET_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .build());

        try {
            refreshCase.handleUSStockMarketCallback(USStockMarketRefreshCallbackCommand.builder()
                    .schemaVersion("us-stock-market-refresh-result/v1")
                    .serverTaskId("us_stock_callback_failure")
                    .idempotencyKey("us_stock_callback_failure:result:1")
                    .status("succeeded")
                    .market(StockMarketEntity.MARKET_US_STOCK)
                    .stocks(List.of(USStockMarketRefreshCallbackCommand.StockMarket.builder()
                            .stockCode("NVDA")
                            .stockName("NVIDIA")
                            .market(StockMarketEntity.MARKET_US_STOCK)
                            .latestPrice("172.41")
                            .build()))
                    .build());
            Assert.fail("should propagate callback processing failure");
        } catch (RuntimeException e) {
            ProcessingTaskEntity task = processingRepository.queryTask("us_stock_callback_failure");
            Assert.assertEquals("callback_failed", task.getStatus().getCode());
            Assert.assertTrue(task.getErrorSummary().contains("stock market persistence failed"));
            Assert.assertFalse(processingRepository.existsNonTerminalTask(ProcessingTaskEntity.US_STOCK_MARKET_REFRESH));
            Assert.assertEquals("failed", processingRepository.callbackProcessStatuses.get("us_stock_callback_failure:us_stock_callback_failure:result:1"));
        }
    }

    private AgentFundRefreshCaseImpl newCase(FakeProcessingRepository processingRepository,
                                             FakeAgentPort agentPort) throws Exception {
        return newCase(processingRepository, agentPort, new FakeStockMarketRepository());
    }

    private AgentFundRefreshCaseImpl newCase(FakeProcessingRepository processingRepository,
                                             FakeAgentPort agentPort,
                                             FakeStockMarketRepository stockMarketRepository) throws Exception {
        AgentFundRefreshCaseImpl refreshCase = new AgentFundRefreshCaseImpl();
        setField(refreshCase, "processingTaskRepository", processingRepository);
        setField(refreshCase, "agentAShareMarketRefreshPort", agentPort);
        setField(refreshCase, "agentUSStockMarketRefreshPort", agentPort);
        setField(refreshCase, "stockMarketRepository", stockMarketRepository);
        setField(refreshCase, "transactionExecutor", new ImmediateTransactionExecutor());
        setField(refreshCase, "aShareMarketCallbackUrl", "http://server/internal/agent/a-share-market-refresh/callback");
        setField(refreshCase, "usStockMarketCallbackUrl", "http://server/internal/agent/us-stock-market-refresh/callback");
        return refreshCase;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class ImmediateTransactionExecutor extends TransactionExecutor {

        @Override
        public <T> T required(Supplier<T> action) {
            return action.get();
        }

        @Override
        public <T> T requiresNew(Supplier<T> action) {
            return action.get();
        }
    }

    private static class FakeAgentPort implements IAgentAShareMarketRefreshPort, IAgentUSStockMarketRefreshPort {
        private final boolean accepted;
        private final String status;
        private AShareMarketRefreshDispatchCommandEntity lastAShareCommand;
        private USStockMarketRefreshDispatchCommandEntity lastUSStockCommand;

        private FakeAgentPort(boolean accepted, String status) {
            this.accepted = accepted;
            this.status = status;
        }

        @Override
        public FundRefreshDispatchResultEntity dispatch(AShareMarketRefreshDispatchCommandEntity commandEntity) {
            lastAShareCommand = commandEntity;
            return dispatchResult();
        }

        @Override
        public FundRefreshDispatchResultEntity dispatch(USStockMarketRefreshDispatchCommandEntity commandEntity) {
            lastUSStockCommand = commandEntity;
            return dispatchResult();
        }

        private FundRefreshDispatchResultEntity dispatchResult() {
            return FundRefreshDispatchResultEntity.builder()
                    .accepted(accepted)
                    .agentStatus(status)
                    .errorSummary(accepted ? null : "rejected")
                    .build();
        }
    }

    private static class FakeProcessingRepository implements IProcessingTaskRepository {
        private final Map<String, ProcessingTaskEntity> tasks = new HashMap<>();
        private final Set<String> callbacks = new java.util.HashSet<>();
        private final Map<String, String> callbackProcessStatuses = new HashMap<>();
        private final List<ProcessingLogEntity> logs = new java.util.ArrayList<>();

        @Override
        public void saveTask(ProcessingTaskEntity taskEntity) {
            tasks.put(taskEntity.getServerTaskId(), copy(taskEntity));
        }

        @Override
        public void updateTask(ProcessingTaskEntity taskEntity) {
            tasks.put(taskEntity.getServerTaskId(), copy(taskEntity));
        }

        @Override
        public ProcessingTaskEntity queryTask(String serverTaskId) {
            return copy(tasks.get(serverTaskId));
        }

        @Override
        public boolean saveCallbackIfAbsent(ProcessingCallbackEntity callbackEntity) {
            String key = callbackEntity.getServerTaskId() + ":" + callbackEntity.getIdempotencyKey();
            boolean added = callbacks.add(key);
            if (added) {
                callbackProcessStatuses.put(key, callbackEntity.getProcessStatus());
            }
            return added;
        }

        @Override
        public void markCallbackProcessed(String serverTaskId, String idempotencyKey, String processStatus, String errorSummary) {
            callbackProcessStatuses.put(serverTaskId + ":" + idempotencyKey, processStatus);
        }

        @Override
        public boolean existsNonTerminalTask(String taskType) {
            return tasks.values().stream()
                    .anyMatch(task -> taskType.equals(task.getTaskType()) && task.getStatus() != null && !task.getStatus().isTerminal());
        }

        @Override
        public void saveLogs(List<ProcessingLogEntity> logs) {
            this.logs.addAll(logs);
        }

        private ProcessingTaskEntity copy(ProcessingTaskEntity task) {
            if (task == null) {
                return null;
            }
            return ProcessingTaskEntity.builder()
                    .id(task.getId())
                    .serverTaskId(task.getServerTaskId())
                    .taskType(task.getTaskType())
                    .taskParamsJson(task.getTaskParamsJson())
                    .status(task.getStatus())
                    .errorSummary(task.getErrorSummary())
                    .createTime(task.getCreateTime())
                    .updateTime(task.getUpdateTime())
                    .build();
        }
    }

    private static class FakeStockMarketRepository implements IStockMarketRepository {
        private int upsertCount;
        private boolean failUpsert;
        private final List<StockMarketEntity> upsertedMarkets = new java.util.ArrayList<>();

        @Override
        public void registerQuoteTargets(List<StockMarketEntity> quoteTargets) {
        }

        @Override
        public void upsertMarkets(List<StockMarketEntity> markets) {
            if (failUpsert) {
                throw new RuntimeException("stock market persistence failed");
            }
            upsertCount += markets.size();
            upsertedMarkets.addAll(markets);
        }

        @Override
        public Map<String, StockMarketEntity> queryByStockKeys(java.util.Collection<String> stockKeys) {
            return Map.of();
        }

        @Override
        public Set<String> queryExistingStockKeys(java.util.Collection<String> stockKeys) {
            return Set.of();
        }
    }

}
