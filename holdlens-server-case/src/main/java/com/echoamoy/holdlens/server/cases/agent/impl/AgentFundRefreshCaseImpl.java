package com.echoamoy.holdlens.server.cases.agent.impl;

import com.alibaba.fastjson.JSON;
import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
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
import com.echoamoy.holdlens.server.types.common.DateTimeUtils;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class AgentFundRefreshCaseImpl implements IAgentFundRefreshCase {

    private static final String A_SHARE_MARKET_TASK_SCHEMA_VERSION = "a-share-market-refresh-task/v1";
    private static final String A_SHARE_MARKET_RESULT_SCHEMA_VERSION = "a-share-market-refresh-result/v1";
    private static final String US_STOCK_MARKET_TASK_SCHEMA_VERSION = "us-stock-market-refresh-task/v1";
    private static final String US_STOCK_MARKET_RESULT_SCHEMA_VERSION = "us-stock-market-refresh-result/v1";
    private static final String A_SHARE_MARKET_MODULE = "a_share_market_refresh";
    private static final String US_STOCK_MARKET_MODULE = "us_stock_market_refresh";
    private static final int STOCK_MARKET_UPSERT_BATCH_SIZE = 500;

    @Resource
    private IProcessingTaskRepository processingTaskRepository;

    @Resource
    private IAgentAShareMarketRefreshPort agentAShareMarketRefreshPort;

    @Resource
    private IAgentUSStockMarketRefreshPort agentUSStockMarketRefreshPort;

    @Resource
    private IStockMarketRepository stockMarketRepository;

    @Resource
    private TransactionExecutor transactionExecutor;

    @Value("${holdlens.agent.a-share-market-callback-url}")
    private String aShareMarketCallbackUrl;

    @Value("${holdlens.agent.us-stock-market-callback-url}")
    private String usStockMarketCallbackUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundRefreshTaskResult createAndDispatchAShareMarket(AShareMarketRefreshCreateCommand command) {
        if (processingTaskRepository.existsNonTerminalTask(ProcessingTaskEntity.A_SHARE_MARKET_REFRESH)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "A 股全量行情刷新任务正在运行");
        }

        ProcessingTaskEntity taskEntity = ProcessingTaskEntity.builder()
                .serverTaskId(newAShareMarketServerTaskId())
                .taskType(ProcessingTaskEntity.A_SHARE_MARKET_REFRESH)
                .taskParamsJson(buildAShareMarketTaskParamsJson(command == null ? null : command.getTrigger()))
                .status(ProcessingTaskStatusEnumVO.CREATED)
                .build();
        processingTaskRepository.saveTask(taskEntity);

        try {
            FundRefreshDispatchResultEntity dispatchResult = agentAShareMarketRefreshPort.dispatch(AShareMarketRefreshDispatchCommandEntity.builder()
                    .schemaVersion(A_SHARE_MARKET_TASK_SCHEMA_VERSION)
                    .serverTaskId(taskEntity.getServerTaskId())
                    .allowNetwork(Boolean.TRUE)
                    .callbackUrl(aShareMarketCallbackUrl)
                    .build());
            if (dispatchResult != null && dispatchResult.isAccepted()) {
                ProcessingTaskStatusEnumVO nextStatus = "running".equals(dispatchResult.getAgentStatus())
                        ? ProcessingTaskStatusEnumVO.RUNNING
                        : ProcessingTaskStatusEnumVO.DISPATCHED;
                taskEntity.transitTo(nextStatus, null);
            } else {
                taskEntity.transitTo(ProcessingTaskStatusEnumVO.DISPATCH_FAILED,
                        safeSummary(dispatchResult == null ? "agent dispatch rejected" : dispatchResult.getErrorSummary()));
            }
        } catch (Exception e) {
            log.warn("A 股全量行情刷新任务下发失败 taskId={}", taskEntity.getServerTaskId());
            taskEntity.transitTo(ProcessingTaskStatusEnumVO.DISPATCH_FAILED, safeSummary(e.getMessage()));
        }
        processingTaskRepository.updateTask(taskEntity);
        return toTaskDTO(taskEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundRefreshTaskResult createAndDispatchUSStockMarket(USStockMarketRefreshCreateCommand command) {
        if (processingTaskRepository.existsNonTerminalTask(ProcessingTaskEntity.US_STOCK_MARKET_REFRESH)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "美股全量行情刷新任务正在运行");
        }

        ProcessingTaskEntity taskEntity = ProcessingTaskEntity.builder()
                .serverTaskId(newUSStockMarketServerTaskId())
                .taskType(ProcessingTaskEntity.US_STOCK_MARKET_REFRESH)
                .taskParamsJson(buildUSStockMarketTaskParamsJson(command == null ? null : command.getTrigger()))
                .status(ProcessingTaskStatusEnumVO.CREATED)
                .build();
        processingTaskRepository.saveTask(taskEntity);

        try {
            FundRefreshDispatchResultEntity dispatchResult = agentUSStockMarketRefreshPort.dispatch(USStockMarketRefreshDispatchCommandEntity.builder()
                    .schemaVersion(US_STOCK_MARKET_TASK_SCHEMA_VERSION)
                    .serverTaskId(taskEntity.getServerTaskId())
                    .allowNetwork(Boolean.TRUE)
                    .callbackUrl(usStockMarketCallbackUrl)
                    .build());
            if (dispatchResult != null && dispatchResult.isAccepted()) {
                ProcessingTaskStatusEnumVO nextStatus = "running".equals(dispatchResult.getAgentStatus())
                        ? ProcessingTaskStatusEnumVO.RUNNING
                        : ProcessingTaskStatusEnumVO.DISPATCHED;
                taskEntity.transitTo(nextStatus, null);
            } else {
                taskEntity.transitTo(ProcessingTaskStatusEnumVO.DISPATCH_FAILED,
                        safeSummary(dispatchResult == null ? "agent dispatch rejected" : dispatchResult.getErrorSummary()));
            }
        } catch (Exception e) {
            log.warn("美股全量行情刷新任务下发失败 taskId={}", taskEntity.getServerTaskId());
            taskEntity.transitTo(ProcessingTaskStatusEnumVO.DISPATCH_FAILED, safeSummary(e.getMessage()));
        }
        processingTaskRepository.updateTask(taskEntity);
        return toTaskDTO(taskEntity);
    }

    @Override
    public FundRefreshTaskResult handleAShareMarketCallback(AShareMarketRefreshCallbackCommand command) {
        if (command == null || isBlank(command.getServerTaskId())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调缺少任务标识");
        }

        ProcessingTaskEntity taskEntity = processingTaskRepository.queryTask(command.getServerTaskId());
        if (taskEntity == null || !ProcessingTaskEntity.A_SHARE_MARKET_REFRESH.equals(taskEntity.getTaskType())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未知 A 股全量行情刷新任务");
        }

        if (!A_SHARE_MARKET_RESULT_SCHEMA_VERSION.equals(command.getSchemaVersion())) {
            markTaskFailed(command.getServerTaskId(), "unsupported schema version");
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "不支持的回调契约版本");
        }

        if (isBlank(command.getIdempotencyKey())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调缺少幂等键");
        }

        try {
            return transactionExecutor.required(() -> processAShareMarketCallback(command));
        } catch (RuntimeException e) {
            recordCallbackProcessingFailure(command.getServerTaskId(), command.getIdempotencyKey(), command.getStatus(), e);
            throw e;
        }
    }

    private FundRefreshTaskResult processAShareMarketCallback(AShareMarketRefreshCallbackCommand command) {
        ProcessingTaskEntity taskEntity = processingTaskRepository.queryTask(command.getServerTaskId());
        if (taskEntity == null || !ProcessingTaskEntity.A_SHARE_MARKET_REFRESH.equals(taskEntity.getTaskType())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未知 A 股全量行情刷新任务");
        }

        boolean firstCallback = processingTaskRepository.saveCallbackIfAbsent(ProcessingCallbackEntity.builder()
                .serverTaskId(command.getServerTaskId())
                .idempotencyKey(command.getIdempotencyKey())
                .callbackStatus(command.getStatus())
                .processStatus("created")
                .errorSummary(safeSummary(command.getErrorSummary()))
                .build());
        if (!firstCallback || taskEntity.isTerminal()) {
            return toTaskDTO(taskEntity);
        }

        ProcessingTaskStatusEnumVO targetStatus = toTaskStatus(command.getStatus());
        if (targetStatus == ProcessingTaskStatusEnumVO.SUCCEEDED
                || targetStatus == ProcessingTaskStatusEnumVO.PARTIAL_FAILED) {
            List<ProcessingLogEntity> diagnostics = new ArrayList<>(toAShareMarketWarnings(command.getServerTaskId(), command.getRefreshWarnings()));
            List<StockMarketEntity> markets = toStockMarkets(command, diagnostics);
            upsertStockMarkets(markets);
            processingTaskRepository.saveLogs(diagnostics);
        }
        taskEntity.transitTo(targetStatus, safeSummary(command.getErrorSummary()));
        processingTaskRepository.updateTask(taskEntity);
        processingTaskRepository.markCallbackProcessed(command.getServerTaskId(), command.getIdempotencyKey(), "processed", null);
        return toTaskDTO(taskEntity);
    }

    @Override
    public FundRefreshTaskResult handleUSStockMarketCallback(USStockMarketRefreshCallbackCommand command) {
        if (command == null || isBlank(command.getServerTaskId())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调缺少任务标识");
        }

        ProcessingTaskEntity taskEntity = processingTaskRepository.queryTask(command.getServerTaskId());
        if (taskEntity == null || !ProcessingTaskEntity.US_STOCK_MARKET_REFRESH.equals(taskEntity.getTaskType())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未知美股全量行情刷新任务");
        }

        if (!US_STOCK_MARKET_RESULT_SCHEMA_VERSION.equals(command.getSchemaVersion())) {
            markTaskFailed(command.getServerTaskId(), "unsupported schema version");
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "不支持的回调契约版本");
        }

        if (isBlank(command.getIdempotencyKey())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调缺少幂等键");
        }

        try {
            return transactionExecutor.required(() -> processUSStockMarketCallback(command));
        } catch (RuntimeException e) {
            recordCallbackProcessingFailure(command.getServerTaskId(), command.getIdempotencyKey(), command.getStatus(), e);
            throw e;
        }
    }

    private FundRefreshTaskResult processUSStockMarketCallback(USStockMarketRefreshCallbackCommand command) {
        ProcessingTaskEntity taskEntity = processingTaskRepository.queryTask(command.getServerTaskId());
        if (taskEntity == null || !ProcessingTaskEntity.US_STOCK_MARKET_REFRESH.equals(taskEntity.getTaskType())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未知美股全量行情刷新任务");
        }

        boolean firstCallback = processingTaskRepository.saveCallbackIfAbsent(ProcessingCallbackEntity.builder()
                .serverTaskId(command.getServerTaskId())
                .idempotencyKey(command.getIdempotencyKey())
                .callbackStatus(command.getStatus())
                .processStatus("created")
                .errorSummary(safeSummary(command.getErrorSummary()))
                .build());
        if (!firstCallback || taskEntity.isTerminal()) {
            return toTaskDTO(taskEntity);
        }

        ProcessingTaskStatusEnumVO targetStatus = toTaskStatus(command.getStatus());
        if (targetStatus == ProcessingTaskStatusEnumVO.SUCCEEDED
                || targetStatus == ProcessingTaskStatusEnumVO.PARTIAL_FAILED) {
            List<ProcessingLogEntity> diagnostics = new ArrayList<>(toUSStockMarketWarnings(command.getServerTaskId(), command.getRefreshWarnings()));
            List<StockMarketEntity> markets = toUSStockMarkets(command, diagnostics);
            upsertStockMarkets(markets);
            processingTaskRepository.saveLogs(diagnostics);
        }
        taskEntity.transitTo(targetStatus, safeSummary(command.getErrorSummary()));
        processingTaskRepository.updateTask(taskEntity);
        processingTaskRepository.markCallbackProcessed(command.getServerTaskId(), command.getIdempotencyKey(), "processed", null);
        return toTaskDTO(taskEntity);
    }

    private void markTaskFailed(String serverTaskId, String errorSummary) {
        try {
            transactionExecutor.requiresNew(() -> {
                ProcessingTaskEntity latestTask = processingTaskRepository.queryTask(serverTaskId);
                if (latestTask != null && !latestTask.isTerminal()) {
                    latestTask.transitTo(ProcessingTaskStatusEnumVO.FAILED, safeSummary(errorSummary));
                    processingTaskRepository.updateTask(latestTask);
                }
                return null;
            });
        } catch (RuntimeException e) {
            log.warn("agent 回调任务失败状态记录失败 taskId={}", serverTaskId);
        }
    }

    private void recordCallbackProcessingFailure(String serverTaskId, String idempotencyKey, String callbackStatus, RuntimeException cause) {
        try {
            transactionExecutor.requiresNew(() -> {
                String errorSummary = safeSummary(cause.getMessage());
                ProcessingTaskEntity latestTask = processingTaskRepository.queryTask(serverTaskId);
                if (latestTask != null && !latestTask.isTerminal()) {
                    latestTask.transitTo(ProcessingTaskStatusEnumVO.CALLBACK_FAILED, errorSummary);
                    processingTaskRepository.updateTask(latestTask);
                }
                recordFailedCallback(serverTaskId, idempotencyKey, callbackStatus, errorSummary);
                return null;
            });
        } catch (RuntimeException e) {
            log.warn("agent 回调处理失败状态记录失败 taskId={}", serverTaskId);
        }
    }

    private void recordFailedCallback(String serverTaskId, String idempotencyKey, String callbackStatus, String errorSummary) {
        if (isBlank(idempotencyKey)) {
            return;
        }
        try {
            processingTaskRepository.saveCallbackIfAbsent(ProcessingCallbackEntity.builder()
                    .serverTaskId(serverTaskId)
                    .idempotencyKey(idempotencyKey)
                    .callbackStatus(defaultString(callbackStatus, "callback_failed"))
                    .processStatus("failed")
                    .errorSummary(errorSummary)
                    .build());
            processingTaskRepository.markCallbackProcessed(serverTaskId, idempotencyKey, "failed", errorSummary);
        } catch (RuntimeException e) {
            // 任务终态优先保证；callback 表异常不应回滚 callback_failed 状态。
            log.warn("agent 回调处理记录失败 taskId={} idempotencyKey={}", serverTaskId, idempotencyKey);
        }
    }

    private List<StockMarketEntity> toStockMarkets(AShareMarketRefreshCallbackCommand command, List<ProcessingLogEntity> diagnostics) {
        if (!StockMarketEntity.MARKET_A_SHARE.equals(command.getMarket())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "A 股全量行情回调 market 必须为 A_SHARE");
        }
        if (command.getStocks() == null) {
            return List.of();
        }
        List<StockMarketEntity> result = new ArrayList<>();
        for (AShareMarketRefreshCallbackCommand.StockMarket stock : command.getStocks()) {
            if (stock == null || isBlank(stock.getStockCode())) {
                diagnostics.add(stockMarketDiagnostic(command.getServerTaskId(), A_SHARE_MARKET_MODULE,
                        "stock_code_missing", "跳过缺少股票代码的 A 股行情记录", "warning"));
                continue;
            }
            if (!StockMarketEntity.MARKET_A_SHARE.equals(stock.getMarket())) {
                diagnostics.add(stockMarketDiagnostic(command.getServerTaskId(), A_SHARE_MARKET_MODULE, "stock_market_unsupported",
                        "跳过非 A_SHARE 股票行情记录: " + stock.getStockCode().trim(), "warning"));
                continue;
            }
            LocalDateTime fallbackRefreshedAt = DateTimeUtils.parseOffsetDateTimeOrNow(command.getGeneratedAt());
            result.add(StockMarketEntity.builder()
                    .stockCode(stock.getStockCode().trim())
                    .market(StockMarketEntity.MARKET_A_SHARE)
                    .exchangeCode(normalizeNullable(stock.getExchangeCode()))
                    .providerMarketCode(normalizeNullable(stock.getProviderMarketCode()))
                    .stockName(normalizeNullable(stock.getStockName()))
                    .currency(StockMarketEntity.CURRENCY_CNY)
                    .volumeUnit(StockMarketEntity.VOLUME_UNIT_LOT)
                    .latestPrice(parseDecimal(stock.getLatestPrice(), command.getServerTaskId(), stock.getStockCode(), "latest_price", diagnostics))
                    .changePercent(parseDecimal(stock.getChangePercent(), command.getServerTaskId(), stock.getStockCode(), "change_percent", diagnostics))
                    .changeAmount(parseDecimal(stock.getChangeAmount(), command.getServerTaskId(), stock.getStockCode(), "change_amount", diagnostics))
                    .volume(parseLong(stock.getVolume(), command.getServerTaskId(), stock.getStockCode(), "volume", diagnostics))
                    .turnoverAmount(parseDecimal(stock.getTurnoverAmount(), command.getServerTaskId(), stock.getStockCode(), "turnover_amount", diagnostics))
                    .amplitude(parseDecimal(stock.getAmplitude(), command.getServerTaskId(), stock.getStockCode(), "amplitude", diagnostics))
                    .highPrice(parseDecimal(stock.getHighPrice(), command.getServerTaskId(), stock.getStockCode(), "high_price", diagnostics))
                    .lowPrice(parseDecimal(stock.getLowPrice(), command.getServerTaskId(), stock.getStockCode(), "low_price", diagnostics))
                    .openPrice(parseDecimal(stock.getOpenPrice(), command.getServerTaskId(), stock.getStockCode(), "open_price", diagnostics))
                    .previousClose(parseDecimal(stock.getPreviousClose(), command.getServerTaskId(), stock.getStockCode(), "previous_close", diagnostics))
                    .volumeRatio(parseDecimal(stock.getVolumeRatio(), command.getServerTaskId(), stock.getStockCode(), "volume_ratio", diagnostics))
                    .turnoverRate(parseDecimal(stock.getTurnoverRate(), command.getServerTaskId(), stock.getStockCode(), "turnover_rate", diagnostics))
                    .peDynamic(parseDecimal(stock.getPeDynamic(), command.getServerTaskId(), stock.getStockCode(), "pe_dynamic", diagnostics))
                    .pbRatio(parseDecimal(stock.getPbRatio(), command.getServerTaskId(), stock.getStockCode(), "pb_ratio", diagnostics))
                    .totalMarketValue(parseDecimal(stock.getTotalMarketValue(), command.getServerTaskId(), stock.getStockCode(), "total_market_value", diagnostics))
                    .circulatingMarketValue(parseDecimal(stock.getCirculatingMarketValue(), command.getServerTaskId(), stock.getStockCode(), "circulating_market_value", diagnostics))
                    .speed(parseDecimal(stock.getSpeed(), command.getServerTaskId(), stock.getStockCode(), "speed", diagnostics))
                    .fiveMinuteChange(parseDecimal(stock.getFiveMinuteChange(), command.getServerTaskId(), stock.getStockCode(), "five_minute_change", diagnostics))
                    .sixtyDayChangePercent(parseDecimal(stock.getSixtyDayChangePercent(), command.getServerTaskId(), stock.getStockCode(), "sixty_day_change_percent", diagnostics))
                    .yearToDateChangePercent(parseDecimal(stock.getYearToDateChangePercent(), command.getServerTaskId(), stock.getStockCode(), "year_to_date_change_percent", diagnostics))
                    .status(StockMarketEntity.STATUS_ACTIVE)
                    .refreshedAt(firstNonNull(DateTimeUtils.parseOffsetDateTimeOrNull(stock.getRefreshedAt()), fallbackRefreshedAt))
                    .build());
        }
        return result;
    }

    private List<StockMarketEntity> toUSStockMarkets(USStockMarketRefreshCallbackCommand command, List<ProcessingLogEntity> diagnostics) {
        if (!StockMarketEntity.MARKET_US_STOCK.equals(command.getMarket())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "美股全量行情回调 market 必须为 US_STOCK");
        }
        if (command.getStocks() == null) {
            return List.of();
        }
        List<StockMarketEntity> result = new ArrayList<>();
        for (USStockMarketRefreshCallbackCommand.StockMarket stock : command.getStocks()) {
            if (stock == null || isBlank(stock.getStockCode())) {
                diagnostics.add(stockMarketDiagnostic(command.getServerTaskId(), US_STOCK_MARKET_MODULE,
                        "stock_code_missing", "跳过缺少股票代码的美股行情记录", "warning"));
                continue;
            }
            if (!StockMarketEntity.MARKET_US_STOCK.equals(stock.getMarket())) {
                diagnostics.add(stockMarketDiagnostic(command.getServerTaskId(), US_STOCK_MARKET_MODULE,
                        "stock_market_unsupported", "跳过非 US_STOCK 股票行情记录: " + stock.getStockCode().trim(), "warning"));
                continue;
            }
            LocalDateTime fallbackRefreshedAt = DateTimeUtils.parseOffsetDateTimeOrNow(command.getGeneratedAt());
            result.add(StockMarketEntity.builder()
                    .stockCode(stock.getStockCode().trim())
                    .market(StockMarketEntity.MARKET_US_STOCK)
                    .exchangeCode(normalizeNullable(stock.getExchangeCode()))
                    .providerMarketCode(normalizeNullable(stock.getProviderMarketCode()))
                    .stockName(normalizeNullable(stock.getStockName()))
                    .currency(StockMarketEntity.CURRENCY_USD)
                    .volumeUnit(StockMarketEntity.VOLUME_UNIT_SHARE)
                    .latestPrice(parseDecimal(stock.getLatestPrice(), command.getServerTaskId(), stock.getStockCode(), "latest_price", US_STOCK_MARKET_MODULE, diagnostics))
                    .changePercent(parseDecimal(stock.getChangePercent(), command.getServerTaskId(), stock.getStockCode(), "change_percent", US_STOCK_MARKET_MODULE, diagnostics))
                    .changeAmount(parseDecimal(stock.getChangeAmount(), command.getServerTaskId(), stock.getStockCode(), "change_amount", US_STOCK_MARKET_MODULE, diagnostics))
                    .volume(parseLong(stock.getVolume(), command.getServerTaskId(), stock.getStockCode(), "volume", US_STOCK_MARKET_MODULE, diagnostics))
                    .turnoverAmount(parseDecimal(stock.getTurnoverAmount(), command.getServerTaskId(), stock.getStockCode(), "turnover_amount", US_STOCK_MARKET_MODULE, diagnostics))
                    .amplitude(parseDecimal(stock.getAmplitude(), command.getServerTaskId(), stock.getStockCode(), "amplitude", US_STOCK_MARKET_MODULE, diagnostics))
                    .highPrice(parseDecimal(stock.getHighPrice(), command.getServerTaskId(), stock.getStockCode(), "high_price", US_STOCK_MARKET_MODULE, diagnostics))
                    .lowPrice(parseDecimal(stock.getLowPrice(), command.getServerTaskId(), stock.getStockCode(), "low_price", US_STOCK_MARKET_MODULE, diagnostics))
                    .openPrice(parseDecimal(stock.getOpenPrice(), command.getServerTaskId(), stock.getStockCode(), "open_price", US_STOCK_MARKET_MODULE, diagnostics))
                    .previousClose(parseDecimal(stock.getPreviousClose(), command.getServerTaskId(), stock.getStockCode(), "previous_close", US_STOCK_MARKET_MODULE, diagnostics))
                    .volumeRatio(parseDecimal(stock.getVolumeRatio(), command.getServerTaskId(), stock.getStockCode(), "volume_ratio", US_STOCK_MARKET_MODULE, diagnostics))
                    .turnoverRate(parseDecimal(stock.getTurnoverRate(), command.getServerTaskId(), stock.getStockCode(), "turnover_rate", US_STOCK_MARKET_MODULE, diagnostics))
                    .peRatio(parseDecimal(stock.getPeRatio(), command.getServerTaskId(), stock.getStockCode(), "pe_ratio", US_STOCK_MARKET_MODULE, diagnostics))
                    .pbRatio(parseDecimal(stock.getPbRatio(), command.getServerTaskId(), stock.getStockCode(), "pb_ratio", US_STOCK_MARKET_MODULE, diagnostics))
                    .totalMarketValue(parseDecimal(stock.getTotalMarketValue(), command.getServerTaskId(), stock.getStockCode(), "total_market_value", US_STOCK_MARKET_MODULE, diagnostics))
                    .circulatingMarketValue(parseDecimal(stock.getCirculatingMarketValue(), command.getServerTaskId(), stock.getStockCode(), "circulating_market_value", US_STOCK_MARKET_MODULE, diagnostics))
                    .speed(parseDecimal(stock.getSpeed(), command.getServerTaskId(), stock.getStockCode(), "speed", US_STOCK_MARKET_MODULE, diagnostics))
                    .fiveMinuteChange(parseDecimal(stock.getFiveMinuteChange(), command.getServerTaskId(), stock.getStockCode(), "five_minute_change", US_STOCK_MARKET_MODULE, diagnostics))
                    .sixtyDayChangePercent(parseDecimal(stock.getSixtyDayChangePercent(), command.getServerTaskId(), stock.getStockCode(), "sixty_day_change_percent", US_STOCK_MARKET_MODULE, diagnostics))
                    .yearToDateChangePercent(parseDecimal(stock.getYearToDateChangePercent(), command.getServerTaskId(), stock.getStockCode(), "year_to_date_change_percent", US_STOCK_MARKET_MODULE, diagnostics))
                    .listingDate(parseListingDate(stock.getListingDate(), command.getServerTaskId(), stock.getStockCode(), diagnostics))
                    .status(StockMarketEntity.STATUS_ACTIVE)
                    .refreshedAt(firstNonNull(DateTimeUtils.parseOffsetDateTimeOrNull(stock.getRefreshedAt()), fallbackRefreshedAt))
                    .build());
        }
        return result;
    }

    private List<ProcessingLogEntity> toAShareMarketWarnings(String serverTaskId,
                                                             List<AShareMarketRefreshCallbackCommand.RefreshWarning> warnings) {
        if (warnings == null) {
            return List.of();
        }
        return warnings.stream()
                .filter(Objects::nonNull)
                .filter(warning -> !isBlank(warning.getModule()) && !isBlank(warning.getEvent()))
                .map(warning -> ProcessingLogEntity.builder()
                        .sourceRefId(serverTaskId)
                        .module(warning.getModule().trim())
                        .event(warning.getEvent().trim())
                        .message(safeSummary(defaultString(warning.getMessage(), warning.getEvent())))
                        .severity(defaultString(warning.getSeverity(), "warning"))
                        .build())
                .toList();
    }

    private List<ProcessingLogEntity> toUSStockMarketWarnings(String serverTaskId,
                                                              List<USStockMarketRefreshCallbackCommand.RefreshWarning> warnings) {
        if (warnings == null) {
            return List.of();
        }
        return warnings.stream()
                .filter(Objects::nonNull)
                .filter(warning -> !isBlank(warning.getModule()) && !isBlank(warning.getEvent()))
                .map(warning -> ProcessingLogEntity.builder()
                        .sourceRefId(serverTaskId)
                        .module(warning.getModule().trim())
                        .event(warning.getEvent().trim())
                        .message(safeSummary(defaultString(warning.getMessage(), warning.getEvent())))
                        .severity(defaultString(warning.getSeverity(), "warning"))
                        .build())
                .toList();
    }

    private void upsertStockMarkets(List<StockMarketEntity> markets) {
        if (markets == null || markets.isEmpty()) {
            return;
        }
        for (int start = 0; start < markets.size(); start += STOCK_MARKET_UPSERT_BATCH_SIZE) {
            int end = Math.min(start + STOCK_MARKET_UPSERT_BATCH_SIZE, markets.size());
            stockMarketRepository.upsertMarkets(markets.subList(start, end));
        }
    }

    private ProcessingTaskStatusEnumVO toTaskStatus(String callbackStatus) {
        if ("succeeded".equals(callbackStatus)) {
            return ProcessingTaskStatusEnumVO.SUCCEEDED;
        }
        if ("partial_failed".equals(callbackStatus)) {
            return ProcessingTaskStatusEnumVO.PARTIAL_FAILED;
        }
        if ("failed".equals(callbackStatus)) {
            return ProcessingTaskStatusEnumVO.FAILED;
        }
        if ("callback_failed".equals(callbackStatus)) {
            return ProcessingTaskStatusEnumVO.CALLBACK_FAILED;
        }
        throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "不支持的回调状态");
    }

    private String newAShareMarketServerTaskId() {
        return "a_share_market_refresh_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String newUSStockMarketServerTaskId() {
        return "us_stock_market_refresh_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildAShareMarketTaskParamsJson(String trigger) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("market", StockMarketEntity.MARKET_A_SHARE);
        params.put("trigger", defaultString(trigger, "system"));
        return JSON.toJSONString(params);
    }

    private String buildUSStockMarketTaskParamsJson(String trigger) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("market", StockMarketEntity.MARKET_US_STOCK);
        params.put("trigger", defaultString(trigger, "system"));
        return JSON.toJSONString(params);
    }

    private BigDecimal parseDecimal(String value, String serverTaskId, String stockCode, String fieldName, List<ProcessingLogEntity> diagnostics) {
        return parseDecimal(value, serverTaskId, stockCode, fieldName, A_SHARE_MARKET_MODULE, diagnostics);
    }

    private BigDecimal parseDecimal(String value, String serverTaskId, String stockCode, String fieldName,
                                    String module, List<ProcessingLogEntity> diagnostics) {
        if (isBlank(value) || "-".equals(value.trim())) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            diagnostics.add(parseDiagnostic(serverTaskId, stockCode, fieldName, module));
            return null;
        }
    }

    private Long parseLong(String value, String serverTaskId, String stockCode, String fieldName, List<ProcessingLogEntity> diagnostics) {
        return parseLong(value, serverTaskId, stockCode, fieldName, A_SHARE_MARKET_MODULE, diagnostics);
    }

    private Long parseLong(String value, String serverTaskId, String stockCode, String fieldName,
                           String module, List<ProcessingLogEntity> diagnostics) {
        if (isBlank(value) || "-".equals(value.trim())) {
            return null;
        }
        try {
            return new BigDecimal(value.trim()).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            diagnostics.add(parseDiagnostic(serverTaskId, stockCode, fieldName, module));
            return null;
        }
    }

    private LocalDate parseListingDate(String value, String serverTaskId, String stockCode, List<ProcessingLogEntity> diagnostics) {
        if (isBlank(value) || "-".equals(value.trim())) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            diagnostics.add(stockMarketDiagnostic(serverTaskId, US_STOCK_MARKET_MODULE, "listing_date_parse_failed",
                    "股票 " + safeSummary(stockCode) + " 字段 listing_date 解析失败，已按 NULL 入库", "warning"));
            return null;
        }
    }

    private ProcessingLogEntity parseDiagnostic(String serverTaskId, String stockCode, String fieldName, String module) {
        return stockMarketDiagnostic(serverTaskId, module, "numeric_parse_failed",
                "股票 " + safeSummary(stockCode) + " 字段 " + fieldName + " 解析失败，已按 NULL 入库", "warning");
    }

    private ProcessingLogEntity stockMarketDiagnostic(String serverTaskId, String module, String event, String message, String severity) {
        return ProcessingLogEntity.builder()
                .sourceRefId(serverTaskId)
                .module(module)
                .event(event)
                .message(safeSummary(message))
                .severity(severity)
                .build();
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private FundRefreshTaskResult toTaskDTO(ProcessingTaskEntity taskEntity) {
        return FundRefreshTaskResult.builder()
                .serverTaskId(taskEntity.getServerTaskId())
                .taskType(taskEntity.getTaskType())
                .status(taskEntity.getStatus() == null ? null : taskEntity.getStatus().getCode())
                .errorSummary(taskEntity.getErrorSummary())
                .createTime(taskEntity.getCreateTime())
                .updateTime(taskEntity.getUpdateTime())
                .build();
    }

    private String safeSummary(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    private String defaultString(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private String normalizeNullable(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
