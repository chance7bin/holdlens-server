package com.echoamoy.holdlens.server.cases.agent.impl;

import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AgentFundRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.funddata.model.entity.FundRefreshTargetEntity;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentAShareMarketRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentFundRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentUSStockMarketRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.repository.IProcessingTaskRepository;
import com.echoamoy.holdlens.server.domain.processing.model.entity.AShareMarketRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchCommandEntity;
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

public class AgentFundRefreshCaseImplTest {

    @Test
    public void createAndDispatchDeduplicatesCodesAndMarksRunning() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeAgentPort agentPort = new FakeAgentPort(true, "running");
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort, fundDataRepository);

        FundRefreshTaskResult result = refreshCase.createAndDispatch(FundRefreshCreateCommand.builder()
                .fundCodes(List.of("000001", "000001", " 161725 "))
                .build());

        Assert.assertEquals("running", result.getStatus());
        ProcessingTaskEntity savedTask = processingRepository.queryTask(result.getServerTaskId());
        Assert.assertTrue(savedTask.getTaskParamsJson().contains("\"fundCodeCount\":2"));
        Assert.assertEquals(List.of("000001", "161725"), agentPort.lastFundCommand.getFundCodes());
    }

    @Test
    public void handleCallbackPersistsFundDataAndRegistersAShareTargetsOnlyOnce() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository();
        FakeStockMarketRepository stockMarketRepository = new FakeStockMarketRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"),
                fundDataRepository, stockMarketRepository);
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("task_1")
                .taskType(ProcessingTaskEntity.FUND_DETAIL_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .build());

        AgentFundRefreshCallbackCommand callback = AgentFundRefreshCallbackCommand.builder()
                .schemaVersion("fund-detail-refresh-result/v2")
                .serverTaskId("task_1")
                .idempotencyKey("task_1:result:1")
                .status("succeeded")
                .generatedAt("2026-06-16T10:00:00Z")
                .funds(List.of(AgentFundRefreshCallbackCommand.FundDetail.builder()
                        .fundCode("000001")
                        .fundName("测试基金")
                        .topHoldings(List.of(
                                AgentFundRefreshCallbackCommand.TopHolding.builder()
                                        .rankNo(1)
                                        .stockName(" 测试股份 ")
                                        .stockCode(" 600000 ")
                                        .market("1")
                                        .build(),
                                AgentFundRefreshCallbackCommand.TopHolding.builder()
                                        .rankNo(2)
                                        .stockName("重复股份")
                                        .stockCode("600000")
                                        .market("0")
                                        .build()))
                        .build()))
                .build();

        refreshCase.handleCallback(callback);
        refreshCase.handleCallback(callback);

        Assert.assertEquals(1, fundDataRepository.saveCount);
        Assert.assertEquals(1, stockMarketRepository.registerCount);
        Assert.assertEquals(1, stockMarketRepository.registeredTargets.size());
        Assert.assertEquals("600000", stockMarketRepository.registeredTargets.get(0).getStockCode());
        Assert.assertEquals(StockMarketEntity.MARKET_A_SHARE, stockMarketRepository.registeredTargets.get(0).getMarket());
        Assert.assertEquals("测试股份", stockMarketRepository.registeredTargets.get(0).getStockName());
    }

    @Test
    public void createAShareMarketTaskDispatchesFullMarketRequest() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeAgentPort agentPort = new FakeAgentPort(true, "accepted");
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort, new FakeFundDataRepository());

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
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort, new FakeFundDataRepository());

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
                new FakeFundDataRepository(), stockMarketRepository);
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
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"),
                new FakeFundDataRepository());
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
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort, new FakeFundDataRepository());

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
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort, new FakeFundDataRepository());

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
                new FakeFundDataRepository(), stockMarketRepository);
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

    private AgentFundRefreshCaseImpl newCase(FakeProcessingRepository processingRepository,
                                             FakeAgentPort agentPort,
                                             FakeFundDataRepository fundDataRepository) throws Exception {
        return newCase(processingRepository, agentPort, fundDataRepository, new FakeStockMarketRepository());
    }

    private AgentFundRefreshCaseImpl newCase(FakeProcessingRepository processingRepository,
                                             FakeAgentPort agentPort,
                                             FakeFundDataRepository fundDataRepository,
                                             FakeStockMarketRepository stockMarketRepository) throws Exception {
        AgentFundRefreshCaseImpl refreshCase = new AgentFundRefreshCaseImpl();
        setField(refreshCase, "processingTaskRepository", processingRepository);
        setField(refreshCase, "agentFundRefreshPort", agentPort);
        setField(refreshCase, "agentAShareMarketRefreshPort", agentPort);
        setField(refreshCase, "agentUSStockMarketRefreshPort", agentPort);
        setField(refreshCase, "fundDataRepository", fundDataRepository);
        setField(refreshCase, "stockMarketRepository", stockMarketRepository);
        setField(refreshCase, "callbackUrl", "http://server/internal/agent/fund-detail-refresh/callback");
        setField(refreshCase, "aShareMarketCallbackUrl", "http://server/internal/agent/a-share-market-refresh/callback");
        setField(refreshCase, "usStockMarketCallbackUrl", "http://server/internal/agent/us-stock-market-refresh/callback");
        return refreshCase;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeAgentPort implements IAgentFundRefreshPort, IAgentAShareMarketRefreshPort, IAgentUSStockMarketRefreshPort {
        private final boolean accepted;
        private final String status;
        private FundRefreshDispatchCommandEntity lastFundCommand;
        private AShareMarketRefreshDispatchCommandEntity lastAShareCommand;
        private USStockMarketRefreshDispatchCommandEntity lastUSStockCommand;

        private FakeAgentPort(boolean accepted, String status) {
            this.accepted = accepted;
            this.status = status;
        }

        @Override
        public FundRefreshDispatchResultEntity dispatch(FundRefreshDispatchCommandEntity commandEntity) {
            lastFundCommand = commandEntity;
            return dispatchResult();
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
            return callbacks.add(callbackEntity.getServerTaskId() + ":" + callbackEntity.getIdempotencyKey());
        }

        @Override
        public void markCallbackProcessed(String serverTaskId, String idempotencyKey, String processStatus, String errorSummary) {
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

    private static class FakeFundDataRepository implements IFundDataRepository {
        private int saveCount;

        @Override
        public void saveCurrentData(FundCurrentDataAggregate aggregate) {
            saveCount++;
            Assert.assertEquals("000001", aggregate.getFunds().get(0).getFundCode());
            Assert.assertEquals(LocalDateTime.of(2026, 6, 16, 18, 0), aggregate.getGeneratedAt());
        }

        @Override
        public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) {
            return Map.of();
        }

        @Override
        public Set<String> queryExistingFundCodes(java.util.Collection<String> fundCodes) {
            return Set.of();
        }

        @Override
        public void registerRefreshTargets(List<FundRefreshTargetEntity> refreshTargets) {
        }

        @Override
        public List<FundRefreshTargetEntity> queryRefreshTargetsAfterId(Long lastId, int limit) {
            return List.of();
        }
    }

    private static class FakeStockMarketRepository implements IStockMarketRepository {
        private int upsertCount;
        private int registerCount;
        private final List<StockMarketEntity> registeredTargets = new java.util.ArrayList<>();
        private final List<StockMarketEntity> upsertedMarkets = new java.util.ArrayList<>();

        @Override
        public void registerQuoteTargets(List<StockMarketEntity> quoteTargets) {
            registerCount++;
            registeredTargets.addAll(quoteTargets);
        }

        @Override
        public void upsertMarkets(List<StockMarketEntity> markets) {
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
