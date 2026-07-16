package com.echoamoy.holdlens.server.cases.agent.impl;

import com.alibaba.fastjson.JSON;
import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
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
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
public class FundSliceRefreshCaseImpl implements IFundSliceRefreshCase {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> CALLBACK_STATUSES = Set.of("succeeded", "partial_failed", "failed");
    private static final Set<String> PURCHASE_STATUSES = Set.of("open", "closed", "limited", "suspended", "unknown");
    private static final Set<String> ASSET_ALLOCATION_TYPES =
            Set.of("stock", "bond", "cash", "fund", "other", "unknown");
    private static final int FUND_CATALOG_UPSERT_BATCH_SIZE = 500;
    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
            "(?i)(api[_-]?key|authorization|callback[_-]?auth|cookie|password|secret|token)\\s*[:=]\\s*[^,\\s;]+");
    private static final Map<String, String> TASK_SCHEMAS = Map.of(
            ProcessingTaskEntity.FUND_CATALOG_REFRESH, "fund-catalog-refresh-task/v1",
            ProcessingTaskEntity.FUND_PURCHASE_STATUS_REFRESH, "fund-purchase-status-refresh-task/v1",
            ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH, "fund-period-return-refresh-task/v1",
            ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, "fund-top-holding-refresh-task/v1",
            ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH, "fund-asset-allocation-refresh-task/v1");
    private static final Map<String, String> RESULT_SCHEMAS = Map.of(
            ProcessingTaskEntity.FUND_CATALOG_REFRESH, "fund-catalog-refresh-result/v1",
            ProcessingTaskEntity.FUND_PURCHASE_STATUS_REFRESH, "fund-purchase-status-refresh-result/v1",
            ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH, "fund-period-return-refresh-result/v1",
            ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, "fund-top-holding-refresh-result/v1",
            ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH, "fund-asset-allocation-refresh-result/v1");
    private static final Map<String, String> CALLBACK_PATHS = Map.of(
            ProcessingTaskEntity.FUND_CATALOG_REFRESH, "/internal/agent/fund-catalog-refresh/callback",
            ProcessingTaskEntity.FUND_PURCHASE_STATUS_REFRESH, "/internal/agent/fund-purchase-status-refresh/callback",
            ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH, "/internal/agent/fund-period-return-refresh/callback",
            ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, "/internal/agent/fund-top-holding-refresh/callback",
            ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH, "/internal/agent/fund-asset-allocation-refresh/callback");

    @Resource private IProcessingTaskRepository processingTaskRepository;
    @Resource private IFundDataRepository fundDataRepository;
    @Resource private IAgentFundSliceRefreshPort agentFundSliceRefreshPort;
    @Resource private TransactionExecutor transactionExecutor;
    @Resource(name = "fundCatalogCallbackExecutor") private Executor fundCatalogCallbackExecutor;

    @Value("${holdlens.agent.server-base-url}")
    private String serverBaseUrl;

    @Override public FundRefreshTaskResult scheduleCatalog(String trigger) {
        return scheduleGlobal(ProcessingTaskEntity.FUND_CATALOG_REFRESH, trigger);
    }

    @Override public FundRefreshTaskResult schedulePurchaseStatus(String trigger) {
        return scheduleGlobal(ProcessingTaskEntity.FUND_PURCHASE_STATUS_REFRESH, trigger);
    }

    @Override public FundRefreshTaskResult schedulePeriodReturn(String trigger) {
        return scheduleGlobal(ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH, trigger);
    }

    @Override
    public List<FundRefreshTaskResult> scheduleTopHoldings(String trigger, int batchSize) {
        if (batchSize <= 0) {
            log.warn("跳过基金重仓刷新，batch-size 无效 batchSize={}", batchSize);
            return List.of();
        }
        if (processingTaskRepository.existsNonTerminalTask(ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH)) {
            log.info("跳过基金重仓刷新，本轮开始前已有非终态任务");
            return List.of();
        }
        List<String> targets = normalizeCodes(fundDataRepository.queryTopHoldingRefreshTargets(
                LocalDateTime.now(BEIJING_ZONE).minusDays(90)));
        List<FundRefreshTaskResult> results = new ArrayList<>();
        for (int start = 0; start < targets.size(); start += batchSize) {
            results.add(dispatchTopHoldings(targets.subList(start, Math.min(start + batchSize, targets.size())), trigger));
        }
        return results;
    }

    @Override
    public FundRefreshTaskResult dispatchTopHoldings(List<String> fundCodes, String trigger) {
        List<String> normalized = normalizeCodes(fundCodes);
        if (normalized.isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "重仓刷新基金代码不能为空");
        }
        return createAndDispatch(ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, normalized, trigger);
    }

    @Override
    public List<FundRefreshTaskResult> scheduleAssetAllocations(String trigger, int batchSize) {
        if (batchSize <= 0) {
            log.warn("跳过基金资产配置刷新，batch-size 无效 batchSize={}", batchSize);
            return List.of();
        }
        if (processingTaskRepository.existsNonTerminalTask(ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH)) {
            log.info("跳过基金资产配置刷新，本轮开始前已有非终态任务");
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now(BEIJING_ZONE);
        List<String> targets = normalizeCodes(fundDataRepository.queryAssetAllocationRefreshTargets(
                now.minusDays(90), latestEndedQuarter(now.toLocalDate()), now.minusDays(7)));
        List<FundRefreshTaskResult> results = new ArrayList<>();
        for (int start = 0; start < targets.size(); start += batchSize) {
            results.add(dispatchAssetAllocations(
                    targets.subList(start, Math.min(start + batchSize, targets.size())), trigger));
        }
        return results;
    }

    @Override
    public FundRefreshTaskResult dispatchAssetAllocations(List<String> fundCodes, String trigger) {
        List<String> normalized = normalizeCodes(fundCodes);
        if (normalized.isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "资产配置刷新基金代码不能为空");
        }
        return createAndDispatch(ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH, normalized, trigger);
    }

    private FundRefreshTaskResult scheduleGlobal(String taskType, String trigger) {
        if (processingTaskRepository.existsNonTerminalTask(taskType)) {
            log.info("跳过基金切片刷新，本轮开始前已有非终态任务 taskType={}", taskType);
            return null;
        }
        return createAndDispatch(taskType, null, trigger);
    }

    private FundRefreshTaskResult createAndDispatch(String taskType, List<String> fundCodes, String trigger) {
        ProcessingTaskEntity task = ProcessingTaskEntity.builder()
                .serverTaskId(taskType + "_" + UUID.randomUUID().toString().replace("-", ""))
                .taskType(taskType)
                .taskParamsJson(JSON.toJSONString(Map.of(
                        "trigger", blankToDefault(trigger, "schedule"),
                        "fund_codes", fundCodes == null ? List.of() : fundCodes)))
                .status(ProcessingTaskStatusEnumVO.CREATED)
                .build();
        processingTaskRepository.saveTask(task);
        try {
            FundRefreshDispatchResultEntity dispatched = agentFundSliceRefreshPort.dispatch(
                    FundSliceRefreshDispatchCommandEntity.builder()
                            .taskType(taskType)
                            .schemaVersion(TASK_SCHEMAS.get(taskType))
                            .serverTaskId(task.getServerTaskId())
                            .fundCodes(fundCodes)
                            .allowNetwork(Boolean.TRUE)
                            .callbackUrl(serverBaseUrl + CALLBACK_PATHS.get(taskType))
                            .build());
            if (dispatched != null && dispatched.isAccepted()) {
                task.transitTo("running".equals(dispatched.getAgentStatus())
                        ? ProcessingTaskStatusEnumVO.RUNNING : ProcessingTaskStatusEnumVO.DISPATCHED, null);
            } else {
                task.transitTo(ProcessingTaskStatusEnumVO.DISPATCH_FAILED,
                        safe(dispatched == null ? "agent dispatch rejected" : dispatched.getErrorSummary()));
            }
        } catch (RuntimeException exception) {
            log.warn("基金切片任务派发失败 taskType={} serverTaskId={}", taskType, task.getServerTaskId());
            task.transitTo(ProcessingTaskStatusEnumVO.DISPATCH_FAILED, safe(exception.getMessage()));
        }
        processingTaskRepository.updateTask(task);
        return toResult(task);
    }

    @Override
    public FundRefreshTaskResult handleCallback(String taskType, FundSliceRefreshCallbackCommand command) {
        validateCallback(taskType, command);
        if (!ProcessingTaskEntity.FUND_CATALOG_REFRESH.equals(taskType)) {
            return transactionExecutor.required(() -> processCallback(taskType, command).result());
        }

        int inputCount = command.getFunds() == null ? 0 : command.getFunds().size();
        long startedNanos = System.nanoTime();
        CallbackAcceptanceOutcome acceptance;
        try {
            acceptance = transactionExecutor.required(() -> acceptCatalogCallback(command));
        } catch (RuntimeException exception) {
            log.warn("基金目录 callback 接收事务失败 serverTaskId={} inputCount={} elapsedMs={} exceptionType={}",
                    command.getServerTaskId(), inputCount, elapsedMillis(startedNanos),
                    exception.getClass().getSimpleName());
            throw exception;
        }

        if (!acceptance.shouldProcess()) {
            log.info("基金目录 callback 已去重，不重复提交异步处理 serverTaskId={} inputCount={} elapsedMs={} status={}",
                    command.getServerTaskId(), inputCount, elapsedMillis(startedNanos), acceptance.result().getStatus());
            return acceptance.result();
        }

        try {
            fundCatalogCallbackExecutor.execute(() -> processCatalogCallbackAsync(command, inputCount));
        } catch (RuntimeException exception) {
            log.error("基金目录 callback 异步任务提交失败 serverTaskId={} inputCount={} elapsedMs={}",
                    command.getServerTaskId(), inputCount, elapsedMillis(startedNanos), exception);
            recordCatalogCallbackProcessingFailure(command, exception);
            ProcessingTaskEntity latestTask = processingTaskRepository.queryTask(command.getServerTaskId());
            return latestTask == null ? acceptance.result() : toResult(latestTask);
        }

        log.info("基金目录 callback 已接收并提交异步处理 serverTaskId={} inputCount={} elapsedMs={}",
                command.getServerTaskId(), inputCount, elapsedMillis(startedNanos));
        return acceptance.result();
    }

    private CallbackAcceptanceOutcome acceptCatalogCallback(FundSliceRefreshCallbackCommand command) {
        // 锁定任务行，避免 timeout 扫描和 callback 接收同时把同一任务推进到不同状态。
        ProcessingTaskEntity task = requireTaskForUpdate(
                ProcessingTaskEntity.FUND_CATALOG_REFRESH, command.getServerTaskId());
        if (task.isTerminal()) {
            return new CallbackAcceptanceOutcome(toResult(task), false);
        }
        task.transitTo(ProcessingTaskStatusEnumVO.RUNNING, null);
        processingTaskRepository.updateTask(task);
        boolean first = processingTaskRepository.saveCallbackIfAbsent(ProcessingCallbackEntity.builder()
                .serverTaskId(command.getServerTaskId())
                .idempotencyKey(command.getIdempotencyKey())
                .callbackStatus(command.getStatus())
                .processStatus("processing")
                .errorSummary(safe(command.getErrorSummary()))
                .build());
        if (!first) {
            return new CallbackAcceptanceOutcome(toResult(task), false);
        }
        return new CallbackAcceptanceOutcome(toResult(task), true);
    }

    private void processCatalogCallbackAsync(FundSliceRefreshCallbackCommand command, int inputCount) {
        long startedNanos = System.nanoTime();
        log.info("开始异步处理基金目录 callback serverTaskId={} inputCount={}", command.getServerTaskId(), inputCount);
        try {
            CallbackProcessingOutcome outcome = transactionExecutor.required(() -> processAcceptedCatalogCallback(command));
            int skippedCount = outcome.deduplicated() ? 0 : Math.max(0, inputCount - outcome.validCount());
            // TransactionTemplate 仅在事务提交成功后返回，成功汇总必须留在该边界之外。
            log.info("基金目录 callback 异步事务已提交 serverTaskId={} inputCount={} validCount={} skippedCount={} "
                            + "batchCount={} deduplicated={} elapsedMs={} status={}",
                    command.getServerTaskId(), inputCount, outcome.validCount(), skippedCount,
                    outcome.batchCount(), outcome.deduplicated(), elapsedMillis(startedNanos),
                    outcome.result().getStatus());
        } catch (RuntimeException exception) {
            log.error("基金目录 callback 异步事务处理失败 serverTaskId={} inputCount={} elapsedMs={}",
                    command.getServerTaskId(), inputCount, elapsedMillis(startedNanos), exception);
            recordCatalogCallbackProcessingFailure(command, exception);
        }
    }

    private CallbackProcessingOutcome processAcceptedCatalogCallback(FundSliceRefreshCallbackCommand command) {
        ProcessingTaskEntity task = requireTask(ProcessingTaskEntity.FUND_CATALOG_REFRESH, command.getServerTaskId());
        if (task.isTerminal()) {
            return new CallbackProcessingOutcome(toResult(task), 0, 0, true);
        }
        return applyCallbackResult(ProcessingTaskEntity.FUND_CATALOG_REFRESH, command, task);
    }

    private CallbackProcessingOutcome processCallback(String taskType, FundSliceRefreshCallbackCommand command) {
        ProcessingTaskEntity task = requireTask(taskType, command.getServerTaskId());
        boolean first = processingTaskRepository.saveCallbackIfAbsent(ProcessingCallbackEntity.builder()
                .serverTaskId(command.getServerTaskId())
                .idempotencyKey(command.getIdempotencyKey())
                .callbackStatus(command.getStatus())
                .processStatus("created")
                .errorSummary(safe(command.getErrorSummary()))
                .build());
        if (!first || task.isTerminal()) {
            return new CallbackProcessingOutcome(toResult(task), 0, 0, true);
        }

        return applyCallbackResult(taskType, command, task);
    }

    private CallbackProcessingOutcome applyCallbackResult(String taskType, FundSliceRefreshCallbackCommand command,
                                                           ProcessingTaskEntity task) {
        List<ProcessingLogEntity> logs = toLogs(command);
        ProcessingTaskStatusEnumVO target = toStatus(command.getStatus());
        int validCount = 0;
        int batchCount = 0;
        if (target != ProcessingTaskStatusEnumVO.FAILED) {
            List<FundSliceRefreshCallbackCommand.FundItem> funds = command.getFunds() == null ? List.of() : command.getFunds();
            boolean inflightNoop = (ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH.equals(taskType)
                    || ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH.equals(taskType))
                    && funds.isEmpty() && hasWarning(command, "fund_already_inflight");
            if (funds.isEmpty() && !inflightNoop) {
                target = ProcessingTaskStatusEnumVO.FAILED;
                logs.add(log(command.getServerTaskId(), taskType, "empty_result", "基金切片结果为空"));
            } else if (!inflightNoop) {
                FundApplyResult applied = applyFunds(
                        taskType, funds, command.getServerTaskId(), logs, fetchedAt(command.getGeneratedAt()));
                validCount = applied.validCount();
                batchCount = applied.batchCount();
                if (validCount == 0) {
                    target = ProcessingTaskStatusEnumVO.FAILED;
                } else if (validCount < funds.size() || !logs.isEmpty()
                        || target == ProcessingTaskStatusEnumVO.PARTIAL_FAILED) {
                    target = ProcessingTaskStatusEnumVO.PARTIAL_FAILED;
                }
            }
        }
        processingTaskRepository.saveLogs(logs);
        task.transitTo(target, safe(command.getErrorSummary()));
        processingTaskRepository.updateTask(task);
        processingTaskRepository.markCallbackProcessed(command.getServerTaskId(), command.getIdempotencyKey(), "processed", null);
        return new CallbackProcessingOutcome(toResult(task), validCount, batchCount, false);
    }

    private void recordCatalogCallbackProcessingFailure(FundSliceRefreshCallbackCommand command, RuntimeException cause) {
        try {
            transactionExecutor.requiresNew(() -> {
                String errorSummary = safe(blank(cause.getMessage())
                        ? cause.getClass().getSimpleName() : cause.getMessage());
                ProcessingTaskEntity latestTask = processingTaskRepository.queryTask(command.getServerTaskId());
                if (latestTask != null && !latestTask.isTerminal()) {
                    latestTask.transitTo(ProcessingTaskStatusEnumVO.CALLBACK_FAILED, errorSummary);
                    processingTaskRepository.updateTask(latestTask);
                }
                processingTaskRepository.markCallbackProcessed(command.getServerTaskId(), command.getIdempotencyKey(),
                        "failed", errorSummary);
                return null;
            });
        } catch (RuntimeException exception) {
            log.error("基金目录 callback 异步失败状态记录失败 serverTaskId={} idempotencyKey={}",
                    command.getServerTaskId(), command.getIdempotencyKey(), exception);
        }
    }

    private FundApplyResult applyFunds(String taskType, List<FundSliceRefreshCallbackCommand.FundItem> funds,
                                       String taskId, List<ProcessingLogEntity> logs, LocalDateTime fetchedAt) {
        if (ProcessingTaskEntity.FUND_CATALOG_REFRESH.equals(taskType)) {
            return applyCatalogFunds(funds, taskId, logs, fetchedAt);
        }
        int valid = 0;
        for (FundSliceRefreshCallbackCommand.FundItem item : funds) {
            if (item == null || blank(item.getFundCode())) {
                logs.add(log(taskId, taskType, "invalid_fund", "基金代码缺失"));
                continue;
            }
            boolean accepted = switch (taskType) {
                case ProcessingTaskEntity.FUND_PURCHASE_STATUS_REFRESH -> applyPurchase(item, fetchedAt);
                case ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH -> applyReturn(item, fetchedAt, logs, taskId);
                case ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH -> applyHolding(item, fetchedAt, logs, taskId);
                case ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH ->
                        applyAssetAllocation(item, fetchedAt, logs, taskId);
                default -> false;
            };
            if (accepted) {
                valid++;
            } else if (!ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH.equals(taskType)
                    && !ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH.equals(taskType)) {
                logs.add(log(taskId, taskType, "row_skipped", "基金记录无效或基金不存在: " + item.getFundCode()));
            }
        }
        return new FundApplyResult(valid, 0);
    }

    private FundApplyResult applyCatalogFunds(List<FundSliceRefreshCallbackCommand.FundItem> funds, String taskId,
                                              List<ProcessingLogEntity> logs, LocalDateTime fetchedAt) {
        int valid = 0;
        int batchCount = 0;
        List<FundCurrentDataAggregate.FundDetail> batch = new ArrayList<>(FUND_CATALOG_UPSERT_BATCH_SIZE);
        for (FundSliceRefreshCallbackCommand.FundItem item : funds) {
            if (item == null || blank(item.getFundCode())) {
                logs.add(log(taskId, ProcessingTaskEntity.FUND_CATALOG_REFRESH, "invalid_fund", "基金代码缺失"));
                continue;
            }
            if (blank(item.getFundName())) {
                logs.add(log(taskId, ProcessingTaskEntity.FUND_CATALOG_REFRESH, "row_skipped",
                        "基金记录无效或基金不存在: " + item.getFundCode()));
                continue;
            }
            batch.add(base(item).fundName(item.getFundName().trim())
                    .fundType(trim(item.getFundType())).pinyinAbbr(trim(item.getPinyinAbbr()))
                    .pinyinFull(trim(item.getPinyinFull())).catalogFetchedAt(fetchedAt).build());
            valid++;
            if (batch.size() == FUND_CATALOG_UPSERT_BATCH_SIZE) {
                upsertCatalogBatch(taskId, batchCount + 1, batch);
                batchCount++;
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            upsertCatalogBatch(taskId, batchCount + 1, batch);
            batchCount++;
        }
        return new FundApplyResult(valid, batchCount);
    }

    private void upsertCatalogBatch(String taskId, int batchIndex,
                                    List<FundCurrentDataAggregate.FundDetail> batch) {
        List<FundCurrentDataAggregate.FundDetail> immutableBatch = List.copyOf(batch);
        long startedNanos = System.nanoTime();
        try {
            fundDataRepository.upsertCatalogs(immutableBatch);
            log.debug("基金目录批次 SQL 执行完成，等待事务提交 serverTaskId={} batchIndex={} batchSize={} elapsedMs={}",
                    taskId, batchIndex, immutableBatch.size(), elapsedMillis(startedNanos));
        } catch (RuntimeException exception) {
            log.warn("基金目录批次 SQL 执行失败 serverTaskId={} batchIndex={} batchSize={} elapsedMs={} exceptionType={}",
                    taskId, batchIndex, immutableBatch.size(), elapsedMillis(startedNanos),
                    exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private boolean applyPurchase(FundSliceRefreshCallbackCommand.FundItem item, LocalDateTime fetchedAt) {
        if (!PURCHASE_STATUSES.contains(item.getBuyStatus())) return false;
        return fundDataRepository.updatePurchaseStatus(base(item).buyStatus(item.getBuyStatus())
                .dailyPurchaseLimit(trim(item.getDailyPurchaseLimit())).purchaseStatusFetchedAt(fetchedAt).build());
    }

    private boolean applyReturn(FundSliceRefreshCallbackCommand.FundItem item, LocalDateTime fetchedAt,
                                List<ProcessingLogEntity> logs, String taskId) {
        if (!Set.of("covered", "source_not_covered", "unknown").contains(item.getCoverageStatus())) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH, "invalid_coverage", "收益覆盖状态无效: " + item.getFundCode()));
            return false;
        }
        Date asOf = parseDate(item.getReturnsAsOf());
        if ("covered".equals(item.getCoverageStatus()) && asOf == null) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH, "invalid_returns_date", "收益日期缺失: " + item.getFundCode()));
            return false;
        }
        FundCurrentDataAggregate.FundDetail current = current(item.getFundCode());
        if (current == null) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH, "unknown_fund", "收益基金不存在: " + item.getFundCode()));
            return false;
        }
        if (asOf != null && current.getReturnsAsOf() != null && asOf.before(current.getReturnsAsOf())) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH, "older_snapshot", "收益日期早于当前值: " + item.getFundCode()));
            return false;
        }
        FundCurrentDataAggregate.FundDetail incoming = base(item)
                .returnCoverageStatus(item.getCoverageStatus()).returnsAsOf(asOf)
                .unitNav(item.getUnitNav()).accumulatedNav(item.getAccumulatedNav()).dailyGrowthRate(item.getDailyGrowthRate())
                .oneMonthReturn(item.getOneMonthReturn()).threeMonthsReturn(item.getThreeMonthsReturn())
                .sixMonthsReturn(item.getSixMonthsReturn()).oneYearReturn(item.getOneYearReturn())
                .threeYearsReturn(item.getThreeYearsReturn()).periodReturnFetchedAt(fetchedAt).build();
        if (sameReturn(current, incoming)) return true;
        return fundDataRepository.updatePeriodReturn(incoming);
    }

    private boolean applyHolding(FundSliceRefreshCallbackCommand.FundItem item, LocalDateTime fetchedAt,
                                 List<ProcessingLogEntity> logs, String taskId) {
        Date asOf = parseDate(item.getTopHoldingsAsOf());
        FundCurrentDataAggregate.FundDetail current = current(item.getFundCode());
        if (current == null || asOf == null) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, "invalid_or_unknown_fund", "重仓基金不存在或报告期缺失: " + item.getFundCode()));
            return false;
        }
        if (current.getTopHoldingsAsOf() != null && asOf.before(current.getTopHoldingsAsOf())) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, "older_snapshot", "重仓报告期早于当前值: " + item.getFundCode()));
            return false;
        }
        boolean clear = "no_public_stock_holdings".equals(item.getPublicHoldingsStatus());
        if (!clear && !"public".equals(item.getPublicHoldingsStatus())) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, "missing_snapshot", "重仓状态不允许覆盖: " + item.getFundCode()));
            return false;
        }
        List<FundCurrentDataAggregate.TopHolding> holdings = normalizeHoldings(item.getFundCode(), item.getTopHoldings());
        if (item.getTopHoldings() != null && holdings.size() < item.getTopHoldings().size()) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, "invalid_holding_rows",
                    "部分重仓行排名或增减类型无效: " + item.getFundCode()));
        }
        if (!clear && holdings.isEmpty()) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, "empty_holdings", "公开重仓列表为空，保留旧数据: " + item.getFundCode()));
            return false;
        }
        FundCurrentDataAggregate.FundDetail incoming = base(item).topHoldingsAsOf(asOf)
                .publicHoldingsStatus(item.getPublicHoldingsStatus()).topHoldingFetchedAt(fetchedAt)
                .topHoldings(holdings).build();
        if (!clear && sameHoldings(current, incoming)) return true;
        return fundDataRepository.updateTopHoldingSnapshot(incoming, clear);
    }

    private boolean applyAssetAllocation(FundSliceRefreshCallbackCommand.FundItem item, LocalDateTime fetchedAt,
                                         List<ProcessingLogEntity> logs, String taskId) {
        FundCurrentDataAggregate.FundDetail current = current(item.getFundCode());
        if (current == null) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                    "unknown_fund", "资产配置基金不存在: " + item.getFundCode()));
            return false;
        }
        if ("unavailable".equals(item.getAllocationStatus())) {
            // unavailable 是 agent 跨报告期探测后的可信结果；有历史时保留，无历史时记录退避时间。
            if ("available".equals(current.getAssetAllocationStatus())) {
                return true;
            }
            fundDataRepository.markAssetAllocationUnavailable(item.getFundCode().trim(), fetchedAt);
            return true;
        }
        if ("missing".equals(item.getAllocationStatus())) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                    "missing_snapshot", "资产配置结果缺失，保留当前值: " + item.getFundCode()));
            return false;
        }
        if (!"available".equals(item.getAllocationStatus())) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                    "invalid_allocation_status", "资产配置状态无效: " + item.getFundCode()));
            return false;
        }

        Date asOf = parseDate(item.getAssetAllocationAsOf());
        if (asOf == null) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                    "invalid_allocation_date", "资产配置报告期缺失或无效: " + item.getFundCode()));
            return false;
        }
        if (current.getAssetAllocationAsOf() != null && asOf.before(current.getAssetAllocationAsOf())) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                    "older_snapshot", "资产配置报告期早于当前值: " + item.getFundCode()));
            return false;
        }

        AllocationNormalization normalization = normalizeAssetAllocations(
                item.getFundCode().trim(), item.getAssetAllocations());
        if (normalization.invalid() || normalization.rows().isEmpty()) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                    "invalid_or_empty_allocations", "资产配置明细无效或为空，保留当前值: " + item.getFundCode()));
            return false;
        }
        FundCurrentDataAggregate.FundDetail incoming = base(item)
                .assetAllocationAsOf(asOf)
                .assetAllocationStatus("available")
                .assetAllocationFetchedAt(fetchedAt)
                .assetAllocations(normalization.rows())
                .build();
        if (sameAssetAllocations(current, incoming)) {
            return true;
        }
        if (!fundDataRepository.replaceAssetAllocationSnapshot(incoming)) {
            logs.add(log(taskId, ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH,
                    "concurrent_older_snapshot", "资产配置已被并发的新报告期更新，本次不覆盖: " + item.getFundCode()));
        }
        return true;
    }

    private AllocationNormalization normalizeAssetAllocations(
            String fundCode, List<FundSliceRefreshCallbackCommand.AssetAllocation> source) {
        if (source == null || source.isEmpty()) {
            return new AllocationNormalization(List.of(), false);
        }
        boolean invalid = false;
        Map<AssetAllocationKey, FundCurrentDataAggregate.AssetAllocation> byTypeAndName = new LinkedHashMap<>();
        for (FundSliceRefreshCallbackCommand.AssetAllocation row : source) {
            if (row == null || blank(row.getAssetType()) || blank(row.getAssetTypeName())
                    || !ASSET_ALLOCATION_TYPES.contains(row.getAssetType().trim())
                    || row.getAllocationRatio() == null || row.getAllocationRatio().signum() < 0
                    || row.getAllocationRatio().compareTo(new java.math.BigDecimal("100")) > 0
                    || row.getDisplayOrder() == null || row.getDisplayOrder() < 1) {
                invalid = true;
                continue;
            }
            String assetType = row.getAssetType().trim();
            String assetTypeName = row.getAssetTypeName().trim();
            byTypeAndName.put(new AssetAllocationKey(assetType, assetTypeName),
                    FundCurrentDataAggregate.AssetAllocation.builder()
                            .fundCode(fundCode)
                            .assetType(assetType)
                            .assetTypeName(assetTypeName)
                            .allocationRatio(row.getAllocationRatio())
                            .displayOrder(row.getDisplayOrder())
                            .build());
        }
        List<FundCurrentDataAggregate.AssetAllocation> rows = byTypeAndName.values().stream()
                .sorted(java.util.Comparator
                        .comparing(FundCurrentDataAggregate.AssetAllocation::getDisplayOrder)
                        .thenComparing(FundCurrentDataAggregate.AssetAllocation::getAssetType)
                        .thenComparing(FundCurrentDataAggregate.AssetAllocation::getAssetTypeName))
                .toList();
        return new AllocationNormalization(rows, invalid);
    }

    private List<FundCurrentDataAggregate.TopHolding> normalizeHoldings(String fundCode,
                                                                        List<FundSliceRefreshCallbackCommand.TopHolding> source) {
        if (source == null) return List.of();
        Map<Integer, FundCurrentDataAggregate.TopHolding> byRank = new LinkedHashMap<>();
        for (FundSliceRefreshCallbackCommand.TopHolding row : source) {
            if (row == null || row.getRankNo() == null || row.getRankNo() < 1 || row.getRankNo() > 10
                    || blank(row.getQuarterChangeType())) continue;
            byRank.put(row.getRankNo(), FundCurrentDataAggregate.TopHolding.builder()
                    .fundCode(fundCode).rankNo(row.getRankNo()).stockName(trim(row.getStockName()))
                    .stockCode(trim(row.getStockCode())).market(trim(row.getMarket()))
                    .holdingRatio(row.getHoldingRatio()).quarterChangeType(trim(row.getQuarterChangeType()))
                    .quarterChangeValue(row.getQuarterChangeValue()).build());
        }
        return byRank.values().stream().sorted(java.util.Comparator.comparing(FundCurrentDataAggregate.TopHolding::getRankNo)).toList();
    }

    private boolean sameReturn(FundCurrentDataAggregate.FundDetail left, FundCurrentDataAggregate.FundDetail right) {
        if (!Objects.equals(left.getReturnCoverageStatus(), right.getReturnCoverageStatus())) return false;
        if (!"covered".equals(right.getReturnCoverageStatus())) return true;
        return
                Objects.equals(left.getReturnsAsOf(), right.getReturnsAsOf())
                && decimalEquals(left.getUnitNav(), right.getUnitNav())
                && decimalEquals(left.getAccumulatedNav(), right.getAccumulatedNav())
                && decimalEquals(left.getDailyGrowthRate(), right.getDailyGrowthRate())
                && decimalEquals(left.getOneMonthReturn(), right.getOneMonthReturn())
                && decimalEquals(left.getThreeMonthsReturn(), right.getThreeMonthsReturn())
                && decimalEquals(left.getSixMonthsReturn(), right.getSixMonthsReturn())
                && decimalEquals(left.getOneYearReturn(), right.getOneYearReturn())
                && decimalEquals(left.getThreeYearsReturn(), right.getThreeYearsReturn());
    }

    private boolean sameHoldings(FundCurrentDataAggregate.FundDetail left, FundCurrentDataAggregate.FundDetail right) {
        if (!Objects.equals(left.getTopHoldingsAsOf(), right.getTopHoldingsAsOf())
                || !Objects.equals(left.getPublicHoldingsStatus(), right.getPublicHoldingsStatus())) return false;
        List<FundCurrentDataAggregate.TopHolding> leftRows = left.getTopHoldings() == null ? List.of() : left.getTopHoldings();
        List<FundCurrentDataAggregate.TopHolding> rightRows = right.getTopHoldings() == null ? List.of() : right.getTopHoldings();
        if (leftRows.size() != rightRows.size()) return false;
        for (int i = 0; i < leftRows.size(); i++) {
            FundCurrentDataAggregate.TopHolding a = leftRows.get(i);
            FundCurrentDataAggregate.TopHolding b = rightRows.get(i);
            if (!Objects.equals(a.getRankNo(), b.getRankNo()) || !Objects.equals(trim(a.getStockName()), trim(b.getStockName()))
                    || !Objects.equals(trim(a.getStockCode()), trim(b.getStockCode())) || !Objects.equals(trim(a.getMarket()), trim(b.getMarket()))
                    || !decimalEquals(a.getHoldingRatio(), b.getHoldingRatio())
                    || !Objects.equals(trim(a.getQuarterChangeType()), trim(b.getQuarterChangeType()))
                    || !decimalEquals(a.getQuarterChangeValue(), b.getQuarterChangeValue())) return false;
        }
        return true;
    }

    private boolean sameAssetAllocations(FundCurrentDataAggregate.FundDetail left,
                                         FundCurrentDataAggregate.FundDetail right) {
        if (!Objects.equals(left.getAssetAllocationAsOf(), right.getAssetAllocationAsOf())
                || !"available".equals(left.getAssetAllocationStatus())) {
            return false;
        }
        List<FundCurrentDataAggregate.AssetAllocation> leftRows = left.getAssetAllocations() == null
                ? List.of() : left.getAssetAllocations().stream()
                .sorted(java.util.Comparator
                        .comparing(FundCurrentDataAggregate.AssetAllocation::getDisplayOrder)
                        .thenComparing(FundCurrentDataAggregate.AssetAllocation::getAssetType)
                        .thenComparing(FundCurrentDataAggregate.AssetAllocation::getAssetTypeName))
                .toList();
        List<FundCurrentDataAggregate.AssetAllocation> rightRows = right.getAssetAllocations() == null
                ? List.of() : right.getAssetAllocations();
        if (leftRows.size() != rightRows.size()) {
            return false;
        }
        for (int i = 0; i < leftRows.size(); i++) {
            FundCurrentDataAggregate.AssetAllocation a = leftRows.get(i);
            FundCurrentDataAggregate.AssetAllocation b = rightRows.get(i);
            if (!Objects.equals(trim(a.getAssetType()), trim(b.getAssetType()))
                    || !Objects.equals(trim(a.getAssetTypeName()), trim(b.getAssetTypeName()))
                    || !decimalEquals(a.getAllocationRatio(), b.getAllocationRatio())
                    || !Objects.equals(a.getDisplayOrder(), b.getDisplayOrder())) {
                return false;
            }
        }
        return true;
    }

    private boolean decimalEquals(java.math.BigDecimal left, java.math.BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private FundCurrentDataAggregate.FundDetail current(String fundCode) {
        return fundDataRepository.queryCurrentDetails(Set.of(fundCode)).get(fundCode);
    }

    @Override
    public int closeTimedOutCallbacks(int timeoutMinutes) {
        if (timeoutMinutes < 5) {
            log.warn("跳过 callback 超时收口，配置不足以覆盖 agent 重试窗口 timeoutMinutes={}", timeoutMinutes);
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now(BEIJING_ZONE).minusMinutes(timeoutMinutes);
        List<ProcessingTaskEntity> tasks = processingTaskRepository.queryNonTerminalFundSliceTasksUpdatedBefore(cutoff);
        int closed = 0;
        for (ProcessingTaskEntity task : tasks) {
            // 条件更新避免 callback 与超时扫描竞争时覆盖已经提交的成功终态。
            if (processingTaskRepository.markCallbackFailedIfTimedOut(
                    task.getServerTaskId(), cutoff, "agent callback timeout")) {
                closed++;
            }
        }
        return closed;
    }

    @Override
    public int warnSlowCatalogCallbacks(int warningMinutes) {
        if (warningMinutes < 1) {
            log.warn("跳过基金目录 callback 慢处理扫描，配置必须大于 0 warningMinutes={}", warningMinutes);
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now(BEIJING_ZONE).minusMinutes(warningMinutes);
        List<ProcessingCallbackEntity> callbacks =
                processingTaskRepository.queryProcessingCatalogCallbacksCreatedBefore(cutoff);
        for (ProcessingCallbackEntity callback : callbacks) {
            log.warn("基金目录 callback 异步处理超过阈值，保持 processing 状态 serverTaskId={} "
                            + "processStatus={} warningMinutes={} callbackCreateTime={}",
                    callback.getServerTaskId(), callback.getProcessStatus(), warningMinutes, callback.getCreateTime());
        }
        return callbacks.size();
    }

    private void validateCallback(String taskType, FundSliceRefreshCallbackCommand command) {
        if (!ProcessingTaskEntity.isFundSliceRefresh(taskType) || command == null || blank(command.getServerTaskId()))
            throw illegal("回调任务信息不完整");
        requireTask(taskType, command.getServerTaskId());
        if (!Objects.equals(RESULT_SCHEMAS.get(taskType), command.getSchemaVersion())) throw illegal("不支持的回调契约版本");
        if (blank(command.getIdempotencyKey())) throw illegal("回调缺少幂等键");
        if (ProcessingTaskEntity.FUND_ASSET_ALLOCATION_REFRESH.equals(taskType)
                && !Objects.equals(command.getServerTaskId() + ":result:1", command.getIdempotencyKey())) {
            throw illegal("资产配置回调幂等键不符合契约");
        }
        if (!CALLBACK_STATUSES.contains(command.getStatus())) throw illegal("回调状态非法");
    }

    private ProcessingTaskEntity requireTask(String taskType, String taskId) {
        ProcessingTaskEntity task = processingTaskRepository.queryTask(taskId);
        if (task == null || !Objects.equals(taskType, task.getTaskType())) throw illegal("未知或类型不匹配的基金切片任务");
        return task;
    }

    private ProcessingTaskEntity requireTaskForUpdate(String taskType, String taskId) {
        ProcessingTaskEntity task = processingTaskRepository.queryTaskForUpdate(taskId);
        if (task == null || !Objects.equals(taskType, task.getTaskType())) throw illegal("未知或类型不匹配的基金切片任务");
        return task;
    }

    private List<ProcessingLogEntity> toLogs(FundSliceRefreshCallbackCommand command) {
        List<ProcessingLogEntity> logs = new ArrayList<>();
        if (command.getRefreshWarnings() != null) for (FundSliceRefreshCallbackCommand.RefreshWarning warning : command.getRefreshWarnings()) {
            if (warning != null) logs.add(ProcessingLogEntity.builder().sourceRefId(command.getServerTaskId())
                    .severity(blankToDefault(warning.getSeverity(), "warning"))
                    .module(safe(warning.getModule())).event(safe(warning.getEvent())).message(safe(warning.getMessage())).build());
        }
        return logs;
    }

    private ProcessingLogEntity log(String taskId, String module, String event, String message) {
        return ProcessingLogEntity.builder().sourceRefId(taskId).module(module).event(event)
                .message(safe(message)).severity("warning").build();
    }

    private boolean hasWarning(FundSliceRefreshCallbackCommand command, String event) {
        return command.getRefreshWarnings() != null && command.getRefreshWarnings().stream()
                .anyMatch(w -> w != null && event.equals(w.getEvent()));
    }

    private FundCurrentDataAggregate.FundDetail.FundDetailBuilder base(FundSliceRefreshCallbackCommand.FundItem item) {
        return FundCurrentDataAggregate.FundDetail.builder().fundCode(item.getFundCode().trim());
    }

    private LocalDateTime fetchedAt(String generatedAt) {
        try { return OffsetDateTime.parse(generatedAt).atZoneSameInstant(BEIJING_ZONE).toLocalDateTime(); }
        catch (RuntimeException ignored) { return LocalDateTime.now(BEIJING_ZONE); }
    }

    private Date parseDate(String value) {
        if (blank(value)) return null;
        try { return java.sql.Date.valueOf(LocalDate.parse(value)); }
        catch (DateTimeParseException ignored) { return null; }
    }

    private LocalDate latestEndedQuarter(LocalDate date) {
        int firstMonthOfQuarter = ((date.getMonthValue() - 1) / 3) * 3 + 1;
        return LocalDate.of(date.getYear(), firstMonthOfQuarter, 1).minusDays(1);
    }

    private ProcessingTaskStatusEnumVO toStatus(String status) {
        return switch (status) {
            case "succeeded" -> ProcessingTaskStatusEnumVO.SUCCEEDED;
            case "partial_failed" -> ProcessingTaskStatusEnumVO.PARTIAL_FAILED;
            case "failed" -> ProcessingTaskStatusEnumVO.FAILED;
            default -> throw illegal("回调状态非法");
        };
    }

    private List<String> normalizeCodes(List<String> source) {
        if (source == null) return List.of();
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (String code : source) if (!blank(code)) codes.add(code.trim());
        return List.copyOf(codes);
    }

    private FundRefreshTaskResult toResult(ProcessingTaskEntity task) {
        return FundRefreshTaskResult.builder().serverTaskId(task.getServerTaskId()).taskType(task.getTaskType())
                .status(task.getStatus().getCode()).errorSummary(task.getErrorSummary())
                .createTime(task.getCreateTime()).updateTime(task.getUpdateTime()).build();
    }

    private AppException illegal(String message) { return new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), message); }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private String trim(String value) { return blank(value) ? null : value.trim(); }
    private String blankToDefault(String value, String fallback) { return blank(value) ? fallback : value.trim(); }
    private long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }
    private String safe(String value) {
        if (value == null) return null;
        String redacted = SENSITIVE_VALUE.matcher(value).replaceAll("$1=[REDACTED]");
        return redacted.substring(0, Math.min(1000, redacted.length()));
    }

    private record FundApplyResult(int validCount, int batchCount) { }

    private record CallbackProcessingOutcome(FundRefreshTaskResult result, int validCount,
                                              int batchCount, boolean deduplicated) { }

    private record CallbackAcceptanceOutcome(FundRefreshTaskResult result, boolean shouldProcess) { }

    private record AssetAllocationKey(String assetType, String assetTypeName) { }

    private record AllocationNormalization(List<FundCurrentDataAggregate.AssetAllocation> rows, boolean invalid) { }
}
