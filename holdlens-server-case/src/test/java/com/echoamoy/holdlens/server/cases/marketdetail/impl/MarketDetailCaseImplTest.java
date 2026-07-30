package com.echoamoy.holdlens.server.cases.marketdetail.impl;

import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailCommand;
import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailResult;
import com.echoamoy.holdlens.server.cases.support.TransactionExecutor;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.marketdetail.adapter.port.IAgentMarketDetailRefreshPort;
import com.echoamoy.holdlens.server.domain.marketdetail.adapter.repository.IMarketDetailRepository;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.FundNavHistoryEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.FundPeriodPerformanceEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.MarketDetailDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.MarketDetailSliceStateEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.StockCompanyProfileEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.StockPriceBarEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.valobj.MarketDetailDispatchResultVO;
import com.echoamoy.holdlens.server.domain.processing.adapter.repository.IProcessingTaskRepository;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingLogEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class MarketDetailCaseImplTest {

    @Test
    public void dispatchesUSProviderCodeAndPersistsValidPartialCallbackIdempotently() throws Exception {
        FakeProcessingRepository processing = new FakeProcessingRepository();
        FakeDetailRepository detail = new FakeDetailRepository();
        FakeAgentPort agent = new FakeAgentPort();
        MarketDetailCaseImpl service = newService(processing, detail, agent);

        MarketDetailResult.Task created = service.createAndDispatch(MarketDetailCommand.CreateTask.builder()
                .assetKind("stock").assetRef("stock:US_STOCK:DEMO")
                .slices(List.of("price_history", "company_profile")).periods(List.of("5d")).build());

        Assert.assertEquals("dispatched", created.getStatus());
        Assert.assertEquals("105.DEMO", agent.command.getProviderMarketCode());
        String taskId = created.getServerTaskId();
        MarketDetailCommand.Callback callback = MarketDetailCommand.Callback.builder()
                .schemaVersion("market-detail-data-refresh-result/v1").serverTaskId(taskId)
                .idempotencyKey(taskId + ":result:1").status("partial_failed")
                .generatedAt("2026-07-17T12:01:00Z").assetKind("stock").assetRef("stock:US_STOCK:DEMO")
                .stockPriceHistories(List.of(MarketDetailCommand.StockPriceHistory.builder()
                        .period("5d").granularity("day").currency("USD")
                        .bars(List.of(MarketDetailCommand.StockBar.builder().barTime("2026-07-17T00:00:00-04:00")
                                .open("10.1").high("10.4").low("10.0").close("10.25").volume("0").build())).build()))
                .stockCompanyProfile(null).build();

        MarketDetailResult.Task completed = service.handleCallback(callback);
        service.handleCallback(callback);

        Assert.assertEquals("partial_failed", completed.getStatus());
        Assert.assertEquals(1, detail.bars.size());
        Assert.assertEquals(0, detail.bars.get(0).getVolume().compareTo(BigDecimal.ZERO));
        Assert.assertEquals(1, processing.callbackCount);
    }

    @Test
    public void dispatchesFundNavSliceUsingSharedContractName() throws Exception {
        FakeAgentPort agent = new FakeAgentPort();
        MarketDetailCaseImpl service = newService(new FakeProcessingRepository(), new FakeDetailRepository(), agent);

        MarketDetailResult.Task created = service.createAndDispatch(MarketDetailCommand.CreateTask.builder()
                .assetKind("fund").assetRef("fund:000001")
                .slices(List.of("nav_history")).periods(List.of()).build());

        Assert.assertEquals("dispatched", created.getStatus());
        Assert.assertEquals(List.of("nav_history"), agent.command.getSlices());
        Assert.assertNull(agent.command.getProviderMarketCode());
    }

    @Test
    public void returnsLatestFiveDailyBarsForFiveDayPeriod() throws Exception {
        FakeDetailRepository detail = new FakeDetailRepository();
        for (int day = 1; day <= 7; day++) {
            detail.bars.add(StockPriceBarEntity.builder().stockCode("DEMO").market("US_STOCK")
                    .granularity("day").barTime(LocalDateTime.of(2026, 7, day, 12, 0))
                    .open(BigDecimal.ONE).high(BigDecimal.ONE).low(BigDecimal.ONE).close(BigDecimal.ONE).build());
        }
        detail.latestBarTime = LocalDateTime.of(2026, 7, 7, 12, 0);
        MarketDetailCaseImpl service = newService(new FakeProcessingRepository(), detail, new FakeAgentPort());

        MarketDetailResult.StockPriceHistory result =
                service.queryStockPriceHistory("stock:US_STOCK:DEMO", "5d");

        Assert.assertEquals(5, result.getPoints().size());
        Assert.assertTrue(result.getPoints().get(0).getBarTime().startsWith("2026-07-03"));
    }

    @Test
    public void redactsSensitiveCallbackDiagnosticsBeforePersistence() throws Exception {
        FakeProcessingRepository processing = new FakeProcessingRepository();
        MarketDetailCaseImpl service = newService(processing, new FakeDetailRepository(), new FakeAgentPort());
        MarketDetailResult.Task created = service.createAndDispatch(MarketDetailCommand.CreateTask.builder()
                .assetKind("stock").assetRef("stock:US_STOCK:DEMO")
                .slices(List.of("company_profile")).periods(List.of()).build());

        service.handleCallback(MarketDetailCommand.Callback.builder()
                .schemaVersion("market-detail-data-refresh-result/v1").serverTaskId(created.getServerTaskId())
                .idempotencyKey(created.getServerTaskId() + ":result:1").status("failed")
                .generatedAt("2026-07-17T12:01:00Z").assetKind("stock").assetRef("stock:US_STOCK:DEMO")
                .errorSummary("Authorization: Bearer secret-value").build());

        Assert.assertFalse(processing.savedCallback.getErrorSummary().contains("secret-value"));
        Assert.assertTrue(processing.savedCallback.getErrorSummary().contains("[redacted]"));
    }

    @Test
    public void acceptsEmptyUnrequestedPriceHistoriesAsAbsentSlice() throws Exception {
        FakeProcessingRepository processing = new FakeProcessingRepository();
        FakeDetailRepository detail = new FakeDetailRepository();
        MarketDetailCaseImpl service = newService(processing, detail, new FakeAgentPort());
        MarketDetailResult.Task created = service.createAndDispatch(MarketDetailCommand.CreateTask.builder()
                .assetKind("stock").assetRef("stock:US_STOCK:DEMO")
                .slices(List.of("company_profile")).periods(List.of()).build());

        MarketDetailResult.Task completed = service.handleCallback(MarketDetailCommand.Callback.builder()
                .schemaVersion("market-detail-data-refresh-result/v1").serverTaskId(created.getServerTaskId())
                .idempotencyKey(created.getServerTaskId() + ":result:1").status("succeeded")
                .generatedAt("2026-07-17T12:01:00Z").assetKind("stock").assetRef("stock:US_STOCK:DEMO")
                .stockPriceHistories(List.of())
                .stockCompanyProfile(MarketDetailCommand.StockCompanyProfile.builder()
                        .companyName("示例公司").industry("软件").build())
                .build());

        Assert.assertEquals("succeeded", completed.getStatus());
        Assert.assertNotNull(detail.profile);
        Assert.assertEquals("示例公司", detail.profile.getCompanyName());
    }

    @Test
    public void recordsRedactedSlicePersistenceFailureDiagnostic() throws Exception {
        FakeProcessingRepository processing = new FakeProcessingRepository();
        FakeDetailRepository detail = new FakeDetailRepository();
        detail.profileFailure = new RuntimeException("password=secret-value, data rejected");
        MarketDetailCaseImpl service = newService(processing, detail, new FakeAgentPort());
        MarketDetailResult.Task created = service.createAndDispatch(MarketDetailCommand.CreateTask.builder()
                .assetKind("stock").assetRef("stock:US_STOCK:DEMO")
                .slices(List.of("company_profile")).periods(List.of()).build());

        MarketDetailResult.Task completed = service.handleCallback(MarketDetailCommand.Callback.builder()
                .schemaVersion("market-detail-data-refresh-result/v1").serverTaskId(created.getServerTaskId())
                .idempotencyKey(created.getServerTaskId() + ":result:1").status("succeeded")
                .generatedAt("2026-07-17T12:01:00Z").assetKind("stock").assetRef("stock:US_STOCK:DEMO")
                .stockCompanyProfile(MarketDetailCommand.StockCompanyProfile.builder().companyName("示例公司").build())
                .build());

        Assert.assertEquals("failed", completed.getStatus());
        Assert.assertEquals(1, processing.logs.size());
        Assert.assertEquals("company_profile_persist_failed", processing.logs.get(0).getEvent());
        Assert.assertTrue(processing.logs.get(0).getMessage().contains("[redacted]"));
        Assert.assertFalse(processing.logs.get(0).getMessage().contains("secret-value"));
    }

    @Test
    public void persistsLongLivedFundHistoryBeyondFiveThousandPoints() throws Exception {
        FakeProcessingRepository processing = new FakeProcessingRepository();
        FakeDetailRepository detail = new FakeDetailRepository();
        MarketDetailCaseImpl service = newService(processing, detail, new FakeAgentPort());
        MarketDetailResult.Task created = service.createAndDispatch(MarketDetailCommand.CreateTask.builder()
                .assetKind("fund").assetRef("fund:000001")
                .slices(List.of("nav_history")).periods(List.of()).build());
        List<MarketDetailCommand.FundNavPoint> points = new ArrayList<>();
        LocalDate firstDate = LocalDate.of(2001, 1, 1);
        for (int index = 0; index < 5001; index++) {
            points.add(MarketDetailCommand.FundNavPoint.builder()
                    .navDate(firstDate.plusDays(index).toString()).unitNav("1.0").build());
        }

        MarketDetailResult.Task completed = service.handleCallback(MarketDetailCommand.Callback.builder()
                .schemaVersion("market-detail-data-refresh-result/v1").serverTaskId(created.getServerTaskId())
                .idempotencyKey(created.getServerTaskId() + ":result:1").status("succeeded")
                .generatedAt("2026-07-17T12:01:00Z").assetKind("fund").assetRef("fund:000001")
                .fundNavHistory(MarketDetailCommand.FundNavHistory.builder().fundCode("000001").points(points).build())
                .build());

        Assert.assertEquals("succeeded", completed.getStatus());
        Assert.assertEquals(5001, detail.navPoints.size());
    }

    @Test
    public void fundDetailRefreshClaimsBothSlicesOnceAndDispatchesEmptyPeriods() throws Exception {
        FakeProcessingRepository processing = new FakeProcessingRepository();
        FakeDetailRepository detail = new FakeDetailRepository();
        FakeAgentPort agent = new FakeAgentPort();
        MarketDetailCaseImpl service = newService(processing, detail, agent);

        MarketDetailResult.FundDetailRefresh first = service.requestFundDetailRefresh("000001");
        String activeTask = detail.states.get("nav_history").getActiveTaskId();
        MarketDetailResult.FundDetailRefresh second = service.requestFundDetailRefresh("000001");

        Assert.assertEquals("refreshing", first.getStatus());
        Assert.assertEquals("refreshing", second.getStatus());
        Assert.assertEquals(1, processing.saveCount);
        Assert.assertEquals(1, agent.dispatchCount);
        Assert.assertEquals(activeTask, detail.states.get("period_performance").getActiveTaskId());
        Assert.assertEquals(List.of("nav_history", "period_performance"), agent.command.getSlices());
        Assert.assertEquals(List.of(), agent.command.getPeriods());
    }

    @Test
    public void successfulFundPerformanceCallbackPersistsSnapshotAndConvergesSlicesIndependently() throws Exception {
        FakeProcessingRepository processing = new FakeProcessingRepository();
        FakeDetailRepository detail = new FakeDetailRepository();
        MarketDetailCaseImpl service = newService(processing, detail, new FakeAgentPort());
        service.requestFundDetailRefresh("000001");
        String taskId = processing.task.getServerTaskId();

        MarketDetailResult.Task completed = service.handleCallback(MarketDetailCommand.Callback.builder()
                .schemaVersion("market-detail-data-refresh-result/v1").serverTaskId(taskId)
                .idempotencyKey(taskId + ":result:1").status("succeeded")
                .generatedAt("2026-07-30T12:01:00Z").assetKind("fund").assetRef("fund:000001")
                .fundNavHistory(MarketDetailCommand.FundNavHistory.builder().fundCode("000001").points(List.of()).build())
                .fundPeriodPerformance(MarketDetailCommand.FundPeriodPerformance.builder().fundCode("000001")
                        .asOf("2026-07-30").rows(List.of(MarketDetailCommand.FundPeriodPerformanceRow.builder()
                                .period("1m").fundReturn("-19.59").peerAverage("-13.16")
                                .peerRank(1560).peerTotal(2318).rankChange(40).build())).build()).build());

        Assert.assertEquals("succeeded", completed.getStatus());
        Assert.assertEquals(1, detail.performanceRows.size());
        Assert.assertEquals("empty", detail.states.get("nav_history").getStatus());
        Assert.assertEquals("available", detail.states.get("period_performance").getStatus());
        Assert.assertNull(detail.states.get("period_performance").getActiveTaskId());
    }

    @Test
    public void dispatchFailureConvergesSlicesAndCooldownPreventsImmediateRetry() throws Exception {
        FakeProcessingRepository processing = new FakeProcessingRepository();
        FakeDetailRepository detail = new FakeDetailRepository();
        FakeAgentPort agent = new FakeAgentPort();
        agent.accepted = false;
        MarketDetailCaseImpl service = newService(processing, detail, agent);

        MarketDetailResult.FundDetailRefresh first = service.requestFundDetailRefresh("000001");
        MarketDetailResult.FundDetailRefresh second = service.requestFundDetailRefresh("000001");

        Assert.assertEquals("unavailable", first.getStatus());
        Assert.assertEquals("unavailable", second.getStatus());
        Assert.assertEquals(1, agent.dispatchCount);
        Assert.assertTrue(second.getSlices().stream().allMatch(slice -> "failed".equals(slice.getStatus())));
    }

    @Test
    public void timedOutTaskConvergesThenCanBeReclaimedAfterCooldown() throws Exception {
        FakeProcessingRepository processing = new FakeProcessingRepository();
        processing.task = ProcessingTaskEntity.builder().serverTaskId("market_detail_data_refresh_old")
                .taskType(ProcessingTaskEntity.MARKET_DETAIL_DATA_REFRESH)
                .status(com.echoamoy.holdlens.server.domain.processing.model.valobj.ProcessingTaskStatusEnumVO.DISPATCHED)
                .build();
        FakeDetailRepository detail = new FakeDetailRepository();
        detail.ensureFundSliceStates("000001", List.of("nav_history", "period_performance"));
        for (MarketDetailSliceStateEntity state : detail.states.values()) {
            state.setStatus("refreshing");
            state.setActiveTaskId(processing.task.getServerTaskId());
            state.setLastAttemptAt(LocalDateTime.now().minusMinutes(20));
        }
        FakeAgentPort agent = new FakeAgentPort();
        MarketDetailCaseImpl service = newService(processing, detail, agent);

        MarketDetailResult.FundDetailRefresh timedOut = service.requestFundDetailRefresh("000001");
        Assert.assertEquals("unavailable", timedOut.getStatus());
        Assert.assertEquals("failed", processing.task.getStatus().getCode());
        Assert.assertEquals(0, agent.dispatchCount);

        for (MarketDetailSliceStateEntity state : detail.states.values()) {
            state.setLastAttemptAt(LocalDateTime.now().minusMinutes(20));
        }
        MarketDetailResult.FundDetailRefresh reclaimed = service.requestFundDetailRefresh("000001");
        Assert.assertEquals("refreshing", reclaimed.getStatus());
        Assert.assertEquals(1, agent.dispatchCount);
        Assert.assertEquals(List.of(), agent.command.getPeriods());
    }

    @Test
    public void periodPerformanceQueryReturnsOnlyRowsFromLatestSnapshot() throws Exception {
        FakeDetailRepository detail = new FakeDetailRepository();
        detail.performanceRows.add(FundPeriodPerformanceEntity.builder().fundCode("000001").period("1m")
                .fundReturn(new BigDecimal("1.23")).asOf(LocalDate.of(2026, 7, 30)).build());
        detail.performanceRows.add(FundPeriodPerformanceEntity.builder().fundCode("000001").period("3m")
                .fundReturn(new BigDecimal("4.56")).asOf(LocalDate.of(2026, 7, 20)).build());
        FakeAgentPort agent = new FakeAgentPort();
        MarketDetailCaseImpl service = newService(new FakeProcessingRepository(), detail, agent);

        MarketDetailResult.FundPeriodPerformance result = service.queryFundPeriodPerformance("000001");

        Assert.assertEquals("2026-07-30", result.getAsOf());
        Assert.assertEquals(1, result.getRows().size());
        Assert.assertEquals("1m", result.getRows().get(0).getPeriod());
        MarketDetailResult.FundDetailRefresh refresh = service.requestFundDetailRefresh("000001");
        Assert.assertEquals("available", refresh.getSlices().get(1).getStatus());
        Assert.assertEquals(List.of("nav_history"), agent.command.getSlices());
    }

    private MarketDetailCaseImpl newService(FakeProcessingRepository processing, FakeDetailRepository detail,
                                             FakeAgentPort agent) throws Exception {
        MarketDetailCaseImpl service = new MarketDetailCaseImpl();
        set(service, "processingTaskRepository", processing);
        set(service, "fundDataRepository", new FakeFundRepository());
        set(service, "stockMarketRepository", new FakeStockRepository());
        set(service, "marketDetailRepository", detail);
        set(service, "agentMarketDetailRefreshPort", agent);
        set(service, "transactionExecutor", new TransactionExecutor() {
            @Override public <T> T required(Supplier<T> action) { return action.get(); }
            @Override public <T> T requiresNew(Supplier<T> action) { return action.get(); }
        });
        set(service, "callbackUrl", "http://127.0.0.1:8091/internal/agent/market-detail-data-refresh/callback");
        set(service, "refreshCooldownMinutes", 10L);
        set(service, "refreshTimeoutMinutes", 10L);
        return service;
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeAgentPort implements IAgentMarketDetailRefreshPort {
        private MarketDetailDispatchCommandEntity command;
        private int dispatchCount;
        private boolean accepted = true;
        @Override public MarketDetailDispatchResultVO dispatch(MarketDetailDispatchCommandEntity command) {
            this.command = command; dispatchCount++;
            return new MarketDetailDispatchResultVO(accepted, accepted ? null : "simulated dispatch rejection");
        }
    }

    private static class FakeProcessingRepository implements IProcessingTaskRepository {
        private ProcessingTaskEntity task;
        private String callbackKey;
        private int callbackCount;
        private ProcessingCallbackEntity savedCallback;
        private int saveCount;
        private final List<ProcessingLogEntity> logs = new ArrayList<>();
        @Override public void saveTask(ProcessingTaskEntity taskEntity) { this.task = taskEntity; saveCount++; }
        @Override public void updateTask(ProcessingTaskEntity taskEntity) { this.task = taskEntity; }
        @Override public ProcessingTaskEntity queryTask(String serverTaskId) { return task; }
        @Override public boolean existsNonTerminalTask(String taskType) { return false; }
        @Override public boolean saveCallbackIfAbsent(ProcessingCallbackEntity callbackEntity) {
            if (callbackEntity.getIdempotencyKey().equals(callbackKey)) return false;
            callbackKey = callbackEntity.getIdempotencyKey(); savedCallback = callbackEntity; callbackCount++; return true;
        }
        @Override public void markCallbackProcessed(String serverTaskId, String idempotencyKey, String processStatus, String errorSummary) { }
        @Override public void saveLogs(List<ProcessingLogEntity> logs) { this.logs.addAll(logs); }
    }

    private static class FakeFundRepository implements IFundDataRepository {
        @Override public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> fundCodes) { return Map.of(); }
        @Override public Set<String> queryExistingFundCodes(Collection<String> fundCodes) { return Set.copyOf(fundCodes); }
        @Override public void upsertCatalogs(List<FundCurrentDataAggregate.FundDetail> funds) { }
    }

    private static class FakeStockRepository implements IStockMarketRepository {
        @Override public void registerQuoteTargets(List<StockMarketEntity> quoteTargets) { }
        @Override public void upsertMarkets(List<StockMarketEntity> markets) { }
        @Override public Map<String, StockMarketEntity> queryByStockKeys(Collection<String> stockKeys) { return Map.of(); }
        @Override public Set<String> queryExistingStockKeys(Collection<String> stockKeys) { return Set.copyOf(stockKeys); }
        @Override public StockMarketEntity queryOne(String stockCode, String market) {
            return StockMarketEntity.builder().stockCode(stockCode).market(market).providerMarketCode("105.DEMO")
                    .currency("USD").stockName("Demo").build();
        }
    }

    private static class FakeDetailRepository implements IMarketDetailRepository {
        private final List<FundNavHistoryEntity> navPoints = new ArrayList<>();
        private final List<StockPriceBarEntity> bars = new ArrayList<>();
        private LocalDateTime latestBarTime;
        private StockCompanyProfileEntity profile;
        private RuntimeException profileFailure;
        private final List<FundPeriodPerformanceEntity> performanceRows = new ArrayList<>();
        private final Map<String, MarketDetailSliceStateEntity> states = new java.util.LinkedHashMap<>();
        @Override public void upsertFundNavHistory(List<FundNavHistoryEntity> points) { navPoints.addAll(points); }
        @Override public void upsertFundPeriodPerformance(List<FundPeriodPerformanceEntity> rows) { performanceRows.addAll(rows); }
        @Override public void upsertStockPriceBars(List<StockPriceBarEntity> values) { bars.addAll(values); }
        @Override public void upsertStockCompanyProfile(StockCompanyProfileEntity profile) {
            if (profileFailure != null) throw profileFailure;
            this.profile = profile;
        }
        @Override public List<FundNavHistoryEntity> queryFundNavHistory(String fundCode, LocalDate startDate) { return List.of(); }
        @Override public LocalDate queryLatestFundNavDate(String fundCode) { return null; }
        @Override public List<FundPeriodPerformanceEntity> queryFundPeriodPerformance(String fundCode) {
            LocalDate latest = performanceRows.stream().filter(row -> fundCode.equals(row.getFundCode()))
                    .map(FundPeriodPerformanceEntity::getAsOf).filter(java.util.Objects::nonNull)
                    .max(LocalDate::compareTo).orElse(null);
            if (latest == null) return List.of();
            return performanceRows.stream().filter(row -> fundCode.equals(row.getFundCode())
                    && latest.equals(row.getAsOf())).toList();
        }
        @Override public void ensureFundSliceStates(String fundCode, List<String> sliceTypes) {
            for (String slice : sliceTypes) states.putIfAbsent(slice, MarketDetailSliceStateEntity.builder()
                    .fundCode(fundCode).sliceType(slice).status("failed").build());
        }
        @Override public MarketDetailSliceStateEntity lockFundSliceState(String fundCode, String sliceType) { return states.get(sliceType); }
        @Override public void updateFundSliceState(MarketDetailSliceStateEntity state) { states.put(state.getSliceType(), state); }
        @Override public boolean updateFundSliceStateIfActiveTask(MarketDetailSliceStateEntity state) {
            MarketDetailSliceStateEntity current = states.get(state.getSliceType());
            if (current == null || !java.util.Objects.equals(current.getActiveTaskId(), state.getActiveTaskId())) return false;
            state.setActiveTaskId(null);
            states.put(state.getSliceType(), state); return true;
        }
        @Override public List<StockPriceBarEntity> queryStockPriceBars(String stockCode, String market, String granularity, LocalDateTime startTime) { return bars; }
        @Override public LocalDateTime queryLatestStockBarTime(String stockCode, String market, String granularity) { return latestBarTime; }
        @Override public StockCompanyProfileEntity queryStockCompanyProfile(String stockCode, String market) { return null; }
    }
}
