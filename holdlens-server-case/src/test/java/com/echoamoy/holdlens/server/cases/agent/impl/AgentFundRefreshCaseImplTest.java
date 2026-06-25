package com.echoamoy.holdlens.server.cases.agent.impl;

import com.echoamoy.holdlens.server.cases.agent.model.AgentFundRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AgentStockQuoteRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentFundRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentStockQuoteRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.repository.IProcessingTaskRepository;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingLogEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.StockQuoteRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.valobj.ProcessingTaskStatusEnumVO;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteEntity;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockQuoteTargetEntity;
import com.echoamoy.holdlens.server.types.exception.AppException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
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
        Assert.assertTrue(savedTask.getTaskParamsJson().contains("\"trigger\":\"system\""));
        Assert.assertEquals(List.of("000001", "161725"), agentPort.lastCommand.getFundCodes());
        Assert.assertEquals(Boolean.TRUE, agentPort.lastCommand.getAllowNetwork());
    }

    @Test
    public void createAndDispatchMarksDispatchFailedWhenAgentRejects() throws Exception {
        AgentFundRefreshCaseImpl refreshCase = newCase(new FakeProcessingRepository(),
                new FakeAgentPort(false, "rejected"), new FakeFundDataRepository());

        FundRefreshTaskResult result = refreshCase.createAndDispatch(FundRefreshCreateCommand.builder()
                .fundCodes(List.of("000001"))
                .build());

        Assert.assertEquals("dispatch_failed", result.getStatus());
    }

    @Test
    public void handleCallbackPersistsOnlyOnceForDuplicateIdempotencyKey() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository();
        FakeStockMarketRepository stockMarketRepository = new FakeStockMarketRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"),
                fundDataRepository, stockMarketRepository);

        ProcessingTaskEntity task = ProcessingTaskEntity.builder()
                .serverTaskId("task_1")
                .taskType(ProcessingTaskEntity.FUND_DETAIL_REFRESH)
                .taskParamsJson("{\"fundCodeCount\":1,\"trigger\":\"system\"}")
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .build();
        processingRepository.saveTask(task);

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
                                        .market(" 1 ")
                                        .build(),
                                AgentFundRefreshCallbackCommand.TopHolding.builder()
                                        .rankNo(2)
                                        .stockName("重复股份")
                                        .stockCode("600000")
                                        .market("1")
                                        .build(),
                                AgentFundRefreshCallbackCommand.TopHolding.builder()
                                        .rankNo(3)
                                        .stockName("缺少代码")
                                        .market("1")
                                        .build(),
	                                AgentFundRefreshCallbackCommand.TopHolding.builder()
	                                        .rankNo(4)
	                                        .stockName("缺少市场")
	                                        .stockCode("000001")
	                                        .build(),
	                                AgentFundRefreshCallbackCommand.TopHolding.builder()
	                                        .rankNo(5)
	                                        .stockName("重复空市场")
	                                        .stockCode(" 000001 ")
	                                        .market(" ")
	                                        .build()))
                        .build()))
                .refreshWarnings(List.of(AgentFundRefreshCallbackCommand.RefreshWarning.builder()
                        .module("fund_refresh")
                        .event("provider_fund_failed")
                        .message("provider failed for one fund")
                        .severity("error")
                        .build()))
                .build();

        FundRefreshTaskResult first = refreshCase.handleCallback(callback);
        FundRefreshTaskResult duplicate = refreshCase.handleCallback(callback);

        Assert.assertEquals("succeeded", first.getStatus());
        Assert.assertEquals("succeeded", duplicate.getStatus());
	        Assert.assertEquals(1, fundDataRepository.saveCount);
	        Assert.assertEquals(1, stockMarketRepository.registerCount);
	        Assert.assertEquals(2, stockMarketRepository.registeredTargets.size());
	        Assert.assertEquals("600000", stockMarketRepository.registeredTargets.get(0).getStockCode());
	        Assert.assertEquals("1", stockMarketRepository.registeredTargets.get(0).getMarket());
	        Assert.assertEquals("测试股份", stockMarketRepository.registeredTargets.get(0).getStockName());
	        Assert.assertEquals("000001", stockMarketRepository.registeredTargets.get(1).getStockCode());
	        Assert.assertNull(stockMarketRepository.registeredTargets.get(1).getMarket());
	        Assert.assertEquals("缺少市场", stockMarketRepository.registeredTargets.get(1).getStockName());
    }

    @Test
    public void handleCallbackFailedUsesMainStatusWithoutSavingFundData() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeFundDataRepository fundDataRepository = new FakeFundDataRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"), fundDataRepository);
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("task_callback_failed")
                .taskType(ProcessingTaskEntity.FUND_DETAIL_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .taskParamsJson("{\"fundCodeCount\":1,\"trigger\":\"system\"}")
                .build());

        FundRefreshTaskResult result = refreshCase.handleCallback(AgentFundRefreshCallbackCommand.builder()
                .schemaVersion("fund-detail-refresh-result/v2")
                .serverTaskId("task_callback_failed")
                .idempotencyKey("task_callback_failed:result:1")
                .status("callback_failed")
                .errorSummary("server callback failed after retries")
                .build());

        Assert.assertEquals("callback_failed", result.getStatus());
        Assert.assertEquals("server callback failed after retries", result.getErrorSummary());
        Assert.assertEquals(0, fundDataRepository.saveCount);
    }

    @Test
    public void handleCallbackRejectsUnsupportedSchemaAndMarksTaskFailed() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"), new FakeFundDataRepository());
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("task_bad_schema")
                .taskType(ProcessingTaskEntity.FUND_DETAIL_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .taskParamsJson("{\"fundCodeCount\":1,\"trigger\":\"system\"}")
                .build());

        try {
            refreshCase.handleCallback(AgentFundRefreshCallbackCommand.builder()
                    .schemaVersion("unknown/v1")
                    .serverTaskId("task_bad_schema")
                    .idempotencyKey("task_bad_schema:result:1")
                    .status("succeeded")
                    .build());
            Assert.fail("should reject unsupported schema");
        } catch (AppException e) {
            Assert.assertEquals("failed", processingRepository.queryTask("task_bad_schema").getStatus().getCode());
        }
    }

    @Test
    public void createStockQuoteTaskUsesCurrentQuoteTargets() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeAgentPort agentPort = new FakeAgentPort(true, "running");
        FakeStockMarketRepository stockMarketRepository = new FakeStockMarketRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, agentPort, new FakeFundDataRepository(), stockMarketRepository);

        FundRefreshTaskResult result = refreshCase.createAndDispatchStockQuotes();

        Assert.assertEquals("running", result.getStatus());
        Assert.assertEquals("stock_quote_refresh", result.getTaskType());
	        Assert.assertEquals(3, agentPort.lastStockCommand.getStocks().size());
	        Assert.assertNull(agentPort.lastStockCommand.getStocks().get(2).getMarket());
	        Assert.assertTrue(processingRepository.queryTask(result.getServerTaskId()).getTaskParamsJson().contains("\"stockCount\":3"));
    }

    @Test
    public void createStockQuoteTaskRejectsEmptyTargets() throws Exception {
        AgentFundRefreshCaseImpl refreshCase = newCase(new FakeProcessingRepository(), new FakeAgentPort(true, "running"),
                new FakeFundDataRepository(), new FakeStockMarketRepository(List.of()));

        try {
            refreshCase.createAndDispatchStockQuotes();
            Assert.fail("should reject empty stock quote targets");
        } catch (AppException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void handleStockQuoteCallbackPersistsQuotesAndWarningsOnce() throws Exception {
        FakeProcessingRepository processingRepository = new FakeProcessingRepository();
        FakeStockMarketRepository stockMarketRepository = new FakeStockMarketRepository();
        AgentFundRefreshCaseImpl refreshCase = newCase(processingRepository, new FakeAgentPort(true, "running"),
                new FakeFundDataRepository(), stockMarketRepository);
        processingRepository.saveTask(ProcessingTaskEntity.builder()
                .serverTaskId("stock_task_1")
                .taskType(ProcessingTaskEntity.STOCK_QUOTE_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING)
                .taskParamsJson("{\"stockCount\":1,\"trigger\":\"system\"}")
                .build());

        AgentStockQuoteRefreshCallbackCommand callback = AgentStockQuoteRefreshCallbackCommand.builder()
                .schemaVersion("stock-quote-refresh-result/v1")
                .serverTaskId("stock_task_1")
                .idempotencyKey("stock_task_1:result:1")
                .status("partial_failed")
                .quotes(List.of(AgentStockQuoteRefreshCallbackCommand.StockQuote.builder()
                                .stockCode("600000")
                                .market("1")
                                .stockName("测试股份")
                                .tradeDate("2026-06-18")
                                .dailyReturn(new java.math.BigDecimal("0.50"))
                                .quoteTime("2026-06-18T10:04:30Z")
                                .build(),
                        AgentStockQuoteRefreshCallbackCommand.StockQuote.builder()
                                .stockCode("000001")
                                .stockName("空市场股份")
                                .tradeDate("2026-06-18")
                                .dailyReturn(new java.math.BigDecimal("0.10"))
                                .quoteTime("2026-06-18T10:05:30Z")
                                .build()))
                .refreshWarnings(List.of(AgentStockQuoteRefreshCallbackCommand.RefreshWarning.builder()
                        .module("stock_quote_refresh")
                        .event("provider_failed")
                        .message("provider failed for one stock")
                        .severity("warning")
                        .build()))
                .build();

        FundRefreshTaskResult first = refreshCase.handleStockQuoteCallback(callback);
        FundRefreshTaskResult duplicate = refreshCase.handleStockQuoteCallback(callback);

        Assert.assertEquals("partial_failed", first.getStatus());
        Assert.assertEquals("partial_failed", duplicate.getStatus());
	        Assert.assertEquals(2, stockMarketRepository.upsertCount);
	        Assert.assertNull(stockMarketRepository.upsertedQuotes.get(1).getMarket());
        Assert.assertEquals(1, processingRepository.logs.size());
        Assert.assertEquals("stock_quote_refresh", processingRepository.logs.get(0).getModule());
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
        setField(refreshCase, "agentStockQuoteRefreshPort", agentPort);
        setField(refreshCase, "fundDataRepository", fundDataRepository);
        setField(refreshCase, "stockMarketRepository", stockMarketRepository);
        setField(refreshCase, "callbackUrl", "http://server/internal/agent/fund-detail-refresh/callback");
        setField(refreshCase, "stockCallbackUrl", "http://server/internal/agent/stock-quote-refresh/callback");
        return refreshCase;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeAgentPort implements IAgentFundRefreshPort, IAgentStockQuoteRefreshPort {
        private final boolean accepted;
        private final String status;
        private FundRefreshDispatchCommandEntity lastCommand;
        private StockQuoteRefreshDispatchCommandEntity lastStockCommand;

        private FakeAgentPort(boolean accepted, String status) {
            this.accepted = accepted;
            this.status = status;
        }

        @Override
        public FundRefreshDispatchResultEntity dispatch(FundRefreshDispatchCommandEntity commandEntity) {
            lastCommand = commandEntity;
            return FundRefreshDispatchResultEntity.builder()
                    .accepted(accepted)
                    .agentStatus(status)
                    .errorSummary(accepted ? null : "rejected")
                    .build();
        }

        @Override
        public FundRefreshDispatchResultEntity dispatch(StockQuoteRefreshDispatchCommandEntity commandEntity) {
            lastStockCommand = commandEntity;
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
            Assert.assertEquals("task_1", aggregate.getSourceRefId());
            if (aggregate.getWarnings() != null && !aggregate.getWarnings().isEmpty()) {
                FundCurrentDataAggregate.RefreshWarning warning = aggregate.getWarnings().get(0);
                Assert.assertEquals("fund_refresh", warning.getModule());
                Assert.assertEquals("provider_fund_failed", warning.getEvent());
                Assert.assertEquals("error", warning.getSeverity());
            }
        }

        @Override
        public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) {
            return Map.of();
        }
    }

    private static class FakeStockMarketRepository implements IStockMarketRepository {
        private final List<StockQuoteTargetEntity> targets;
        private int upsertCount;
        private int registerCount;
        private final List<StockQuoteEntity> registeredTargets = new java.util.ArrayList<>();
        private final List<StockQuoteEntity> upsertedQuotes = new java.util.ArrayList<>();

        private FakeStockMarketRepository() {
            this(List.of(
                    StockQuoteTargetEntity.builder().stockCode("600000").market("1").build(),
                    StockQuoteTargetEntity.builder().stockCode("000001").market("0").build(),
                    StockQuoteTargetEntity.builder().stockCode("000002").market(null).build()));
        }

        private FakeStockMarketRepository(List<StockQuoteTargetEntity> targets) {
            this.targets = targets;
        }

        @Override
        public List<StockQuoteTargetEntity> queryAllQuoteTargets() {
            return targets;
        }

        @Override
        public void registerQuoteTargets(List<StockQuoteEntity> quoteTargets) {
            registerCount++;
            registeredTargets.addAll(quoteTargets);
        }

        @Override
        public void upsertQuotes(List<StockQuoteEntity> quotes) {
            upsertCount += quotes.size();
            upsertedQuotes.addAll(quotes);
        }

        @Override
        public Map<String, StockQuoteEntity> queryByStockKeys(java.util.Collection<String> stockKeys) {
            return Map.of();
        }
    }

}
