package com.echoamoy.holdlens.server.cases.agent.impl;

import com.echoamoy.holdlens.server.cases.agent.model.FundSliceRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.support.TransactionExecutor;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
import com.echoamoy.holdlens.server.domain.processing.adapter.port.IAgentFundSliceRefreshPort;
import com.echoamoy.holdlens.server.domain.processing.adapter.repository.IProcessingTaskRepository;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundRefreshDispatchResultEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.FundSliceRefreshDispatchCommandEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingCallbackEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingLogEntity;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.domain.processing.model.valobj.ProcessingTaskStatusEnumVO;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

public class FundSliceRefreshCaseImplTest {

    @Test
    public void globalTasksUseIndependentSchemasAndSkipActiveType() throws Exception {
        Fixture fixture = fixture();
        fixture.caseImpl.scheduleCatalog("schedule");
        fixture.caseImpl.schedulePurchaseStatus("schedule");
        fixture.caseImpl.schedulePeriodReturn("schedule");
        Assert.assertEquals(3, fixture.port.commands.size());
        Assert.assertEquals("fund-catalog-refresh-task/v1", fixture.port.commands.get(0).getSchemaVersion());
        Assert.assertEquals("fund-purchase-status-refresh-task/v1", fixture.port.commands.get(1).getSchemaVersion());
        Assert.assertEquals("fund-period-return-refresh-task/v1", fixture.port.commands.get(2).getSchemaVersion());
        fixture.caseImpl.scheduleCatalog("schedule");
        Assert.assertEquals(3, fixture.port.commands.size());
    }

    @Test
    public void topHoldingScheduleBatchesTwentyWithoutBlockingCurrentRound() throws Exception {
        Fixture fixture = fixture();
        for (int i = 1; i <= 41; i++) fixture.funds.targets.add(String.format("%06d", i));
        Assert.assertEquals(3, fixture.caseImpl.scheduleTopHoldings("schedule", 20).size());
        Assert.assertEquals(20, fixture.port.commands.get(0).getFundCodes().size());
        Assert.assertEquals(20, fixture.port.commands.get(1).getFundCodes().size());
        Assert.assertEquals(1, fixture.port.commands.get(2).getFundCodes().size());
    }

    @Test
    public void assetAllocationScheduleUsesIndependentSchemaAndBatchesTargets() throws Exception {
        Fixture fixture = fixture();
        for (int i = 1; i <= 41; i++) fixture.funds.allocationTargets.add(String.format("%06d", i));

        Assert.assertEquals(3, fixture.caseImpl.scheduleAssetAllocations("schedule", 20).size());
        Assert.assertEquals("fund-asset-allocation-refresh-task/v1", fixture.port.commands.get(0).getSchemaVersion());
        Assert.assertEquals(20, fixture.port.commands.get(0).getFundCodes().size());
        Assert.assertEquals(1, fixture.port.commands.get(2).getFundCodes().size());
        Assert.assertNotNull(fixture.funds.latestEndedQuarter);
        Assert.assertNotNull(fixture.funds.unavailableRetryBefore);
        ProcessingTaskEntity created = fixture.processing.tasks.values().stream()
                .filter(task -> ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH.equals(task.getTaskType()))
                .findFirst().orElseThrow();
        Assert.assertFalse(created.getTaskParamsJson().contains("asset_allocation_as_of"));
        Assert.assertFalse(created.getTaskParamsJson().toLowerCase().contains("token"));
        Assert.assertFalse(created.getTaskParamsJson().contains("amount"));
        Assert.assertTrue(ProcessingTaskEntity.isFundSliceRefresh(created.getTaskType()));
    }

    @Test
    public void assetAllocationReplacesNewPeriodNoopsSameContentAndCorrectsSamePeriod() throws Exception {
        Fixture fixture = fixture();
        fixture.funds.current.put("000001", FundCurrentDataAggregate.FundDetail.builder()
                .fundCode("000001").assetAllocationStatus("missing").assetAllocations(List.of()).build());

        String first = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();
        Assert.assertEquals("succeeded", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(first, "fund-asset-allocation-refresh-result/v1", "succeeded",
                        List.of(allocationItem("000001", "2026-06-30", "72.3500")))).getStatus());
        Assert.assertEquals(1, fixture.funds.allocationWrites);

        String same = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();
        fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(same, "fund-asset-allocation-refresh-result/v1", "succeeded",
                        List.of(allocationItem("000001", "2026-06-30", "72.350"))));
        Assert.assertEquals(1, fixture.funds.allocationWrites);

        String correction = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();
        fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(correction, "fund-asset-allocation-refresh-result/v1", "succeeded",
                        List.of(allocationItem("000001", "2026-06-30", "73.0000"))));
        Assert.assertEquals(2, fixture.funds.allocationWrites);
        Assert.assertEquals(0, new BigDecimal("73.0000").compareTo(
                fixture.funds.lastAllocation.getAssetAllocations().get(0).getAllocationRatio()));
    }

    @Test
    public void assetAllocationKeepsDifferentRawNamesForUnknownAndRejectsOldOrEmptySnapshots() throws Exception {
        Fixture fixture = fixture();
        fixture.funds.current.put("000001", FundCurrentDataAggregate.FundDetail.builder()
                .fundCode("000001").assetAllocationAsOf(java.sql.Date.valueOf("2026-06-30"))
                .assetAllocationStatus("available")
                .assetAllocations(List.of(FundCurrentDataAggregate.AssetAllocation.builder()
                        .fundCode("000001").assetType("stock").assetTypeName("股票")
                        .allocationRatio(new BigDecimal("70")).displayOrder(1).build())).build());

        FundSliceRefreshCallbackCommand.FundItem corrected = allocationItem("000001", "2026-06-30", "72");
        corrected.setAssetAllocations(List.of(
                allocation("unknown", "其他资产A", "1.1", 1),
                allocation("unknown", "其他资产B", "2.2", 2)));
        String correction = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();
        fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(correction, "fund-asset-allocation-refresh-result/v1", "succeeded", List.of(corrected)));
        Assert.assertEquals(2, fixture.funds.lastAllocation.getAssetAllocations().size());

        String older = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();
        Assert.assertEquals("failed", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(older, "fund-asset-allocation-refresh-result/v1", "succeeded",
                        List.of(allocationItem("000001", "2026-03-31", "60")))).getStatus());
        Assert.assertEquals(1, fixture.funds.allocationWrites);

        FundSliceRefreshCallbackCommand.FundItem empty = allocationItem("000001", "2026-09-30", "60");
        empty.setAssetAllocations(List.of());
        String emptyTask = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();
        Assert.assertEquals("failed", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(emptyTask, "fund-asset-allocation-refresh-result/v1", "succeeded", List.of(empty))).getStatus());
        Assert.assertEquals(1, fixture.funds.allocationWrites);
    }

    @Test
    public void unavailableAndInflightAreTrustedSuccessWhileUnknownFundMakesPartialFailure() throws Exception {
        Fixture fixture = fixture();
        fixture.funds.current.put("000001", FundCurrentDataAggregate.FundDetail.builder()
                .fundCode("000001").assetAllocationStatus("available")
                .assetAllocations(List.of(FundCurrentDataAggregate.AssetAllocation.builder()
                        .assetType("stock").assetTypeName("股票").allocationRatio(BigDecimal.TEN).displayOrder(1).build()))
                .build());
        fixture.funds.current.put("000002", FundCurrentDataAggregate.FundDetail.builder()
                .fundCode("000002").assetAllocationStatus("missing").assetAllocations(List.of()).build());

        String taskId = fixture.caseImpl.dispatchAssetAllocations(List.of("000001", "000002"), "schedule").getServerTaskId();
        List<FundSliceRefreshCallbackCommand.FundItem> unavailable = List.of(
                FundSliceRefreshCallbackCommand.FundItem.builder().fundCode("000001")
                        .allocationStatus("unavailable").assetAllocations(List.of()).build(),
                FundSliceRefreshCallbackCommand.FundItem.builder().fundCode("000002")
                        .allocationStatus("unavailable").assetAllocations(List.of()).build());
        Assert.assertEquals("succeeded", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(taskId, "fund-asset-allocation-refresh-result/v1", "succeeded", unavailable)).getStatus());
        Assert.assertEquals(1, fixture.funds.unavailableWrites);
        Assert.assertEquals("available", fixture.funds.current.get("000001").getAssetAllocationStatus());

        String partial = fixture.caseImpl.dispatchAssetAllocations(List.of("000002", "999999"), "schedule").getServerTaskId();
        Assert.assertEquals("partial_failed", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(partial, "fund-asset-allocation-refresh-result/v1", "succeeded", List.of(
                        allocationItem("000002", "2026-06-30", "50"),
                        allocationItem("999999", "2026-06-30", "50")))).getStatus());

        String inflight = fixture.caseImpl.dispatchAssetAllocations(List.of("000002"), "schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand inflightCallback = callback(
                inflight, "fund-asset-allocation-refresh-result/v1", "succeeded", List.of());
        inflightCallback.setRefreshWarnings(List.of(FundSliceRefreshCallbackCommand.RefreshWarning.builder()
                .module("fund_asset_allocation_refresh").event("fund_already_inflight")
                .severity("warning").message("overlap").build()));
        Assert.assertEquals("succeeded", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH, inflightCallback).getStatus());
    }

    @Test
    public void assetAllocationRequiresExactIdempotencyKeyAndValidPositiveDisplayOrder() throws Exception {
        Fixture fixture = fixture();
        fixture.funds.current.put("000001", FundCurrentDataAggregate.FundDetail.builder()
                .fundCode("000001").assetAllocationStatus("missing").build());
        String taskId = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand invalidKey = callback(
                taskId, "fund-asset-allocation-refresh-result/v1", "succeeded",
                List.of(allocationItem("000001", "2026-06-30", "50")));
        invalidKey.setIdempotencyKey(taskId + ":other");
        try {
            fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH, invalidKey);
            Assert.fail("non-contract idempotency key must be rejected");
        } catch (RuntimeException expected) {
            Assert.assertEquals(0, fixture.funds.allocationWrites);
        }

        String invalidOrderTask = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand.FundItem invalidOrder = allocationItem("000001", "2026-06-30", "50");
        invalidOrder.getAssetAllocations().get(0).setDisplayOrder(0);
        Assert.assertEquals("failed", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(invalidOrderTask, "fund-asset-allocation-refresh-result/v1", "succeeded",
                        List.of(invalidOrder))).getStatus());

        String invalidRatioTask = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand.FundItem invalidRatio = allocationItem("000001", "2026-06-30", "100.0001");
        Assert.assertEquals("failed", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(invalidRatioTask, "fund-asset-allocation-refresh-result/v1", "succeeded",
                        List.of(invalidRatio))).getStatus());

        String invalidTypeTask = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand.FundItem invalidType = allocationItem("000001", "2026-06-30", "50");
        invalidType.getAssetAllocations().get(0).setAssetType("commodity");
        Assert.assertEquals("failed", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(invalidTypeTask, "fund-asset-allocation-refresh-result/v1", "succeeded",
                        List.of(invalidType))).getStatus());
    }

    @Test
    public void concurrentNewerSnapshotGuardBecomesStaleNoop() throws Exception {
        Fixture fixture = fixture();
        fixture.funds.current.put("000001", FundCurrentDataAggregate.FundDetail.builder()
                .fundCode("000001").assetAllocationAsOf(java.sql.Date.valueOf("2026-03-31"))
                .assetAllocationStatus("available").assetAllocations(List.of()).build());
        fixture.funds.replaceAllowed = false;
        String taskId = fixture.caseImpl.dispatchAssetAllocations(List.of("000001"), "schedule").getServerTaskId();

        Assert.assertEquals("partial_failed", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                callback(taskId, "fund-asset-allocation-refresh-result/v1", "succeeded",
                        List.of(allocationItem("000001", "2026-06-30", "50")))).getStatus());
        Assert.assertEquals(0, fixture.funds.allocationWrites);
    }

    @Test
    public void emptyCatalogResultFailsWithoutWrites() throws Exception {
        Fixture fixture = fixture();
        String taskId = fixture.caseImpl.scheduleCatalog("schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand callback = callback(taskId, "fund-catalog-refresh-result/v1", "succeeded", List.of());
        Assert.assertEquals("running", fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_CATALOG_REFRESH, callback).getStatus());
        Assert.assertEquals(0, fixture.funds.catalogWrites);
        fixture.executor.runNext();
        Assert.assertEquals("failed", fixture.processing.tasks.get(taskId).getStatus().getCode());
    }

    @Test
    public void catalogCallbackUpsertsOneThousandAndOneFundsInFiveHundredItemBatches() throws Exception {
        Fixture fixture = fixture();
        String taskId = fixture.caseImpl.scheduleCatalog("schedule").getServerTaskId();
        List<FundSliceRefreshCallbackCommand.FundItem> funds = catalogFunds(1001);

        String acceptedStatus = fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_CATALOG_REFRESH,
                callback(taskId, "fund-catalog-refresh-result/v1", "succeeded", funds)).getStatus();

        Assert.assertEquals("running", acceptedStatus);
        Assert.assertEquals(0, fixture.funds.catalogWrites);
        fixture.executor.runNext();
        Assert.assertEquals("succeeded", fixture.processing.tasks.get(taskId).getStatus().getCode());
        Assert.assertEquals(List.of(500, 500, 1), fixture.funds.catalogBatchSizes);
        Assert.assertEquals(1001, fixture.funds.catalogWrites);
    }

    @Test
    public void catalogBatchFailureStopsLaterBatchesAndRecordsCallbackFailure() throws Exception {
        Fixture fixture = fixture();
        fixture.funds.failCatalogBatchIndex = 2;
        String taskId = fixture.caseImpl.scheduleCatalog("schedule").getServerTaskId();

        Assert.assertEquals("running", fixture.caseImpl.handleCallback(
                ProcessingTaskEntity.FUND_CATALOG_REFRESH,
                callback(taskId, "fund-catalog-refresh-result/v1", "succeeded", catalogFunds(1001))).getStatus());
        fixture.executor.runNext();

        Assert.assertEquals(List.of(500, 500), fixture.funds.catalogBatchAttempts);
        Assert.assertEquals(List.of(500), fixture.funds.catalogBatchSizes);
        Assert.assertEquals("callback_failed", fixture.processing.tasks.get(taskId).getStatus().getCode());
        Assert.assertEquals("failed", fixture.processing.callback(taskId).getProcessStatus());
    }

    @Test
    public void sourceNotCoveredIsValidAndDoesNotClearReturnValues() throws Exception {
        Fixture fixture = fixture();
        fixture.funds.current.put("000001", FundCurrentDataAggregate.FundDetail.builder().fundCode("000001").build());
        String taskId = fixture.caseImpl.schedulePeriodReturn("schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand.FundItem item = FundSliceRefreshCallbackCommand.FundItem.builder()
                .fundCode("000001").coverageStatus("source_not_covered").build();
        Assert.assertEquals("succeeded", fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH,
                callback(taskId, "fund-period-return-refresh-result/v1", "succeeded", List.of(item))).getStatus());
        Assert.assertEquals(1, fixture.funds.returnWrites);
        Assert.assertEquals("source_not_covered", fixture.funds.lastReturn.getReturnCoverageStatus());
        Assert.assertNull(fixture.funds.lastReturn.getReturnsAsOf());
    }

    @Test
    public void ordinaryEmptyHoldingsNeverClearButExplicitNoPublicMayClear() throws Exception {
        Fixture fixture = fixture();
        fixture.funds.current.put("000001", FundCurrentDataAggregate.FundDetail.builder().fundCode("000001")
                .topHoldingsAsOf(java.sql.Date.valueOf("2026-03-31")).publicHoldingsStatus("public")
                .topHoldings(List.of(FundCurrentDataAggregate.TopHolding.builder().rankNo(1).stockCode("600000").build())).build());
        String first = fixture.caseImpl.dispatchTopHoldings(List.of("000001"), "schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand.FundItem empty = FundSliceRefreshCallbackCommand.FundItem.builder()
                .fundCode("000001").topHoldingsAsOf("2026-06-30").publicHoldingsStatus("public").topHoldings(List.of()).build();
        Assert.assertEquals("failed", fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH,
                callback(first, "fund-top-holding-refresh-result/v1", "succeeded", List.of(empty))).getStatus());
        Assert.assertEquals(0, fixture.funds.holdingWrites);

        String second = fixture.caseImpl.dispatchTopHoldings(List.of("000001"), "detail_view").getServerTaskId();
        FundSliceRefreshCallbackCommand.FundItem noPublic = FundSliceRefreshCallbackCommand.FundItem.builder()
                .fundCode("000001").topHoldingsAsOf("2026-06-30")
                .publicHoldingsStatus("no_public_stock_holdings").topHoldings(List.of()).build();
        Assert.assertEquals("succeeded", fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH,
                callback(second, "fund-top-holding-refresh-result/v1", "succeeded", List.of(noPublic))).getStatus());
        Assert.assertTrue(fixture.funds.lastClear);
    }

    @Test
    public void fullyInflightOverlapIsSuccessfulNoopAndCallbackFailedIsRejected() throws Exception {
        Fixture fixture = fixture();
        String taskId = fixture.caseImpl.dispatchTopHoldings(List.of("000001"), "detail_view").getServerTaskId();
        FundSliceRefreshCallbackCommand callback = callback(taskId, "fund-top-holding-refresh-result/v1", "succeeded", List.of());
        callback.setRefreshWarnings(List.of(FundSliceRefreshCallbackCommand.RefreshWarning.builder()
                .module("fund_top_holding_refresh").event("fund_already_inflight").severity("warning").message("overlap").build()));
        Assert.assertEquals("succeeded", fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, callback).getStatus());

        String another = fixture.caseImpl.dispatchTopHoldings(List.of("000002"), "detail_view").getServerTaskId();
        FundSliceRefreshCallbackCommand invalid = callback(another, "fund-top-holding-refresh-result/v1", "callback_failed", List.of());
        try {
            fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, invalid);
            Assert.fail("callback_failed must be rejected");
        } catch (RuntimeException expected) {
            Assert.assertEquals("dispatched", fixture.processing.tasks.get(another).getStatus().getCode());
        }
    }

    @Test
    public void callbackIsIdempotentAndTimeoutOnlyClosesNonTerminalTasks() throws Exception {
        Fixture fixture = fixture();
        String taskId = fixture.caseImpl.scheduleCatalog("schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand.FundItem item = FundSliceRefreshCallbackCommand.FundItem.builder()
                .fundCode("000001").fundName("测试基金").build();
        FundSliceRefreshCallbackCommand callback = callback(taskId, "fund-catalog-refresh-result/v1", "succeeded", List.of(item));
        fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_CATALOG_REFRESH, callback);
        fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_CATALOG_REFRESH, callback);
        Assert.assertEquals(1, fixture.executor.size());
        fixture.executor.runNext();
        Assert.assertEquals(1, fixture.funds.catalogWrites);

        ProcessingTaskEntity old = ProcessingTaskEntity.builder().serverTaskId("old").taskType(ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH)
                .status(ProcessingTaskStatusEnumVO.RUNNING).build();
        fixture.processing.timedOut.add(old);
        Assert.assertEquals(1, fixture.caseImpl.closeTimedOutCallbacks(30));
        Assert.assertTrue(fixture.processing.callbackFailed.contains("old"));
        Assert.assertEquals(0, fixture.caseImpl.closeTimedOutCallbacks(1));
    }

    @Test
    public void callbackDiagnosticsRedactSensitiveValuesBeforePersistence() throws Exception {
        Fixture fixture = fixture();
        String taskId = fixture.caseImpl.scheduleCatalog("schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand callback = callback(taskId, "fund-catalog-refresh-result/v1", "succeeded",
                List.of(FundSliceRefreshCallbackCommand.FundItem.builder()
                        .fundCode("000001").fundName("测试基金").build()));
        callback.setRefreshWarnings(List.of(FundSliceRefreshCallbackCommand.RefreshWarning.builder()
                .module("fund_catalog_refresh").event("source_warning").severity("warning")
                .message("token=secret-value cookie=session-value").build()));

        fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_CATALOG_REFRESH, callback);
        fixture.executor.runNext();

        String persisted = fixture.processing.logs.get(0).getMessage();
        Assert.assertFalse(persisted.contains("secret-value"));
        Assert.assertFalse(persisted.contains("session-value"));
        Assert.assertTrue(persisted.contains("[REDACTED]"));
    }

    @Test
    public void slowCatalogCallbackOnlyWarnsAndKeepsProcessingState() throws Exception {
        Fixture fixture = fixture();
        String taskId = fixture.caseImpl.scheduleCatalog("schedule").getServerTaskId();
        FundSliceRefreshCallbackCommand callback = callback(taskId, "fund-catalog-refresh-result/v1", "succeeded",
                List.of(FundSliceRefreshCallbackCommand.FundItem.builder()
                        .fundCode("000001").fundName("测试基金").build()));

        fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_CATALOG_REFRESH, callback);
        fixture.processing.slowCallbacks.add(fixture.processing.callback(taskId));

        Assert.assertEquals(1, fixture.caseImpl.warnSlowCatalogCallbacks(10));
        Assert.assertEquals("running", fixture.processing.tasks.get(taskId).getStatus().getCode());
        Assert.assertEquals("processing", fixture.processing.callback(taskId).getProcessStatus());
        Assert.assertFalse(fixture.processing.callbackFailed.contains(taskId));
    }

    @Test
    public void rejectedCatalogCallbackExecutionRecordsFailureWithoutRunningOnRequestThread() throws Exception {
        Fixture fixture = fixture();
        fixture.executor.reject = true;
        String taskId = fixture.caseImpl.scheduleCatalog("schedule").getServerTaskId();

        String status = fixture.caseImpl.handleCallback(ProcessingTaskEntity.FUND_CATALOG_REFRESH,
                callback(taskId, "fund-catalog-refresh-result/v1", "succeeded", catalogFunds(1))).getStatus();

        Assert.assertEquals("callback_failed", status);
        Assert.assertEquals(0, fixture.funds.catalogWrites);
        Assert.assertEquals("failed", fixture.processing.callback(taskId).getProcessStatus());
    }

    private FundSliceRefreshCallbackCommand callback(String taskId, String schema, String status,
                                                      List<FundSliceRefreshCallbackCommand.FundItem> funds) {
        return FundSliceRefreshCallbackCommand.builder().serverTaskId(taskId).schemaVersion(schema)
                .idempotencyKey(taskId + ":result:1").status(status).generatedAt("2026-07-12T02:00:00+08:00")
                .funds(funds).refreshWarnings(List.of()).build();
    }

    private List<FundSliceRefreshCallbackCommand.FundItem> catalogFunds(int count) {
        List<FundSliceRefreshCallbackCommand.FundItem> funds = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            funds.add(FundSliceRefreshCallbackCommand.FundItem.builder()
                    .fundCode(String.format("%06d", index))
                    .fundName("测试基金" + index)
                    .build());
        }
        return funds;
    }

    private FundSliceRefreshCallbackCommand.FundItem allocationItem(
            String fundCode, String asOf, String stockRatio) {
        return FundSliceRefreshCallbackCommand.FundItem.builder()
                .fundCode(fundCode)
                .assetAllocationAsOf(asOf)
                .allocationStatus("available")
                .assetAllocations(List.of(allocation("stock", "股票", stockRatio, 1)))
                .build();
    }

    private FundSliceRefreshCallbackCommand.AssetAllocation allocation(
            String assetType, String assetTypeName, String ratio, int displayOrder) {
        return FundSliceRefreshCallbackCommand.AssetAllocation.builder()
                .assetType(assetType).assetTypeName(assetTypeName)
                .allocationRatio(new BigDecimal(ratio)).displayOrder(displayOrder).build();
    }

    private Fixture fixture() throws Exception {
        FundSliceRefreshCaseImpl impl = new FundSliceRefreshCaseImpl();
        FakeProcessing processing = new FakeProcessing();
        FakeFunds funds = new FakeFunds();
        FakePort port = new FakePort();
        QueuedExecutor executor = new QueuedExecutor();
        set(impl, "processingTaskRepository", processing);
        set(impl, "fundDataRepository", funds);
        set(impl, "agentFundSliceRefreshPort", port);
        set(impl, "transactionExecutor", new DirectTransactionExecutor());
        set(impl, "fundCatalogCallbackExecutor", executor);
        set(impl, "serverBaseUrl", "http://server");
        return new Fixture(impl, processing, funds, port, executor);
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private record Fixture(FundSliceRefreshCaseImpl caseImpl, FakeProcessing processing, FakeFunds funds,
                           FakePort port, QueuedExecutor executor) { }

    private static class DirectTransactionExecutor extends TransactionExecutor {
        @Override public <T> T required(Supplier<T> action) { return action.get(); }
        @Override public <T> T requiresNew(Supplier<T> action) { return action.get(); }
    }

    private static class QueuedExecutor implements Executor {
        final List<Runnable> tasks = new ArrayList<>();
        boolean reject;
        @Override public void execute(Runnable command) {
            if (reject) throw new RejectedExecutionException("simulated rejection");
            tasks.add(command);
        }
        void runNext() { tasks.remove(0).run(); }
        int size() { return tasks.size(); }
    }

    private static class FakePort implements IAgentFundSliceRefreshPort {
        final List<FundSliceRefreshDispatchCommandEntity> commands = new ArrayList<>();
        public FundRefreshDispatchResultEntity dispatch(FundSliceRefreshDispatchCommandEntity command) {
            commands.add(command);
            return FundRefreshDispatchResultEntity.builder().accepted(true).agentStatus("accepted").build();
        }
    }

    private static class FakeProcessing implements IProcessingTaskRepository {
        final Map<String, ProcessingTaskEntity> tasks = new HashMap<>();
        final Map<String, ProcessingCallbackEntity> callbacks = new HashMap<>();
        final List<ProcessingTaskEntity> timedOut = new ArrayList<>();
        final List<ProcessingCallbackEntity> slowCallbacks = new ArrayList<>();
        final List<ProcessingLogEntity> logs = new ArrayList<>();
        final Set<String> callbackFailed = new HashSet<>();
        public void saveTask(ProcessingTaskEntity task) { tasks.put(task.getServerTaskId(), task); }
        public void updateTask(ProcessingTaskEntity task) { tasks.put(task.getServerTaskId(), task); }
        public ProcessingTaskEntity queryTask(String id) { return tasks.get(id); }
        public boolean existsNonTerminalTask(String type) { return tasks.values().stream().anyMatch(t -> type.equals(t.getTaskType()) && !t.isTerminal()); }
        public boolean saveCallbackIfAbsent(ProcessingCallbackEntity callback) {
            return callbacks.putIfAbsent(callback.getServerTaskId() + "#" + callback.getIdempotencyKey(), callback) == null;
        }
        public void markCallbackProcessed(String serverTaskId, String key, String status, String error) {
            ProcessingCallbackEntity callback = callbacks.get(serverTaskId + "#" + key);
            if (callback != null) {
                callback.setProcessStatus(status);
                callback.setErrorSummary(error);
            }
        }
        public void saveLogs(List<ProcessingLogEntity> logs) { this.logs.addAll(logs); }
        public List<ProcessingTaskEntity> queryNonTerminalFundSliceTasksUpdatedBefore(LocalDateTime cutoff) { return List.copyOf(timedOut); }
        public List<ProcessingCallbackEntity> queryProcessingCatalogCallbacksCreatedBefore(LocalDateTime cutoff) {
            return List.copyOf(slowCallbacks);
        }
        public boolean markCallbackFailedIfTimedOut(String serverTaskId, LocalDateTime cutoff, String errorSummary) {
            ProcessingTaskEntity task = timedOut.stream().filter(item -> serverTaskId.equals(item.getServerTaskId())).findFirst().orElse(null);
            if (task == null || task.isTerminal()) return false;
            callbackFailed.add(serverTaskId);
            return true;
        }
        ProcessingCallbackEntity callback(String serverTaskId) {
            return callbacks.values().stream().filter(item -> serverTaskId.equals(item.getServerTaskId()))
                    .findFirst().orElseThrow();
        }
    }

    private static class FakeFunds implements IFundDataRepository {
        final Map<String, FundCurrentDataAggregate.FundDetail> current = new HashMap<>();
        final List<String> targets = new ArrayList<>();
        final List<String> allocationTargets = new ArrayList<>();
        final List<Integer> catalogBatchAttempts = new ArrayList<>();
        final List<Integer> catalogBatchSizes = new ArrayList<>();
        int failCatalogBatchIndex;
        int catalogWrites;
        int returnWrites;
        int holdingWrites;
        int allocationWrites;
        int unavailableWrites;
        boolean lastClear;
        FundCurrentDataAggregate.FundDetail lastReturn;
        FundCurrentDataAggregate.FundDetail lastAllocation;
        LocalDate latestEndedQuarter;
        LocalDateTime unavailableRetryBefore;
        boolean replaceAllowed = true;
        public Map<String, FundCurrentDataAggregate.FundDetail> queryCurrentDetails(Set<String> codes) {
            Map<String, FundCurrentDataAggregate.FundDetail> result = new HashMap<>();
            for (String code : codes) if (current.containsKey(code)) result.put(code, current.get(code));
            return result;
        }
        public Set<String> queryExistingFundCodes(Collection<String> codes) { return current.keySet(); }
        public void upsertCatalog(FundCurrentDataAggregate.FundDetail fund) { catalogWrites++; current.put(fund.getFundCode(), fund); }
        public void upsertCatalogs(List<FundCurrentDataAggregate.FundDetail> funds) {
            catalogBatchAttempts.add(funds.size());
            if (catalogBatchAttempts.size() == failCatalogBatchIndex) {
                throw new IllegalStateException("simulated catalog batch failure");
            }
            catalogBatchSizes.add(funds.size());
            funds.forEach(this::upsertCatalog);
        }
        public boolean updatePurchaseStatus(FundCurrentDataAggregate.FundDetail fund) { return current.containsKey(fund.getFundCode()); }
        public boolean updatePeriodReturn(FundCurrentDataAggregate.FundDetail fund) { returnWrites++; lastReturn = fund; return current.containsKey(fund.getFundCode()); }
        public boolean updateTopHoldingSnapshot(FundCurrentDataAggregate.FundDetail fund, boolean clear) { holdingWrites++; lastClear = clear; return current.containsKey(fund.getFundCode()); }
        public List<String> queryTopHoldingRefreshTargets(LocalDateTime since) { return targets; }
        public List<String> queryAssetAllocationRefreshTargets(LocalDateTime since, LocalDate latestEndedQuarter,
                                                               LocalDateTime unavailableRetryBefore) {
            this.latestEndedQuarter = latestEndedQuarter;
            this.unavailableRetryBefore = unavailableRetryBefore;
            return allocationTargets;
        }
        public boolean replaceAssetAllocationSnapshot(FundCurrentDataAggregate.FundDetail fund) {
            if (!replaceAllowed) return false;
            allocationWrites++;
            lastAllocation = fund;
            current.put(fund.getFundCode(), fund);
            return true;
        }
        public boolean markAssetAllocationUnavailable(String fundCode, LocalDateTime fetchedAt) {
            unavailableWrites++;
            FundCurrentDataAggregate.FundDetail existing = current.get(fundCode);
            existing.setAssetAllocationStatus("unavailable");
            existing.setAssetAllocationFetchedAt(fetchedAt);
            return true;
        }
        public void markDetailViewed(Collection<String> codes, LocalDateTime at) { }
    }
}
