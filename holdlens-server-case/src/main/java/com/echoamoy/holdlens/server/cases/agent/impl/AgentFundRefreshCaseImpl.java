package com.echoamoy.holdlens.server.cases.agent.impl;

import com.alibaba.fastjson.JSON;
import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
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
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class AgentFundRefreshCaseImpl implements IAgentFundRefreshCase {

    private static final String TASK_SCHEMA_VERSION = "fund-detail-refresh-task/v1";
    private static final String RESULT_SCHEMA_VERSION = "fund-detail-refresh-result/v2";
    private static final String STOCK_TASK_SCHEMA_VERSION = "stock-quote-refresh-task/v1";
    private static final String STOCK_RESULT_SCHEMA_VERSION = "stock-quote-refresh-result/v1";

    @Resource
    private IProcessingTaskRepository processingTaskRepository;

    @Resource
    private IAgentFundRefreshPort agentFundRefreshPort;

    @Resource
    private IAgentStockQuoteRefreshPort agentStockQuoteRefreshPort;

    @Resource
    private IFundDataRepository fundDataRepository;

    @Resource
    private IStockMarketRepository stockMarketRepository;

    @Value("${holdlens.agent.callback-url:http://127.0.0.1:8091/internal/agent/fund-detail-refresh/callback}")
    private String callbackUrl;

    @Value("${holdlens.agent.stock-callback-url:http://127.0.0.1:8091/internal/agent/stock-quote-refresh/callback}")
    private String stockCallbackUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundRefreshTaskResult createAndDispatch(FundRefreshCreateCommand command) {
        List<String> fundCodes = normalizeFundCodes(command == null ? null : command.getFundCodes());
        if (fundCodes.isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "基金代码不能为空");
        }

        ProcessingTaskEntity taskEntity = ProcessingTaskEntity.builder()
                .serverTaskId(newServerTaskId())
                .taskType(ProcessingTaskEntity.FUND_DETAIL_REFRESH)
                .taskParamsJson(buildTaskParamsJson(fundCodes.size()))
                .status(ProcessingTaskStatusEnumVO.CREATED)
                .build();
        processingTaskRepository.saveTask(taskEntity);

        try {
            FundRefreshDispatchResultEntity dispatchResult = agentFundRefreshPort.dispatch(FundRefreshDispatchCommandEntity.builder()
                    .schemaVersion(TASK_SCHEMA_VERSION)
                    .serverTaskId(taskEntity.getServerTaskId())
                    .fundCodes(fundCodes)
                    .allowNetwork(Boolean.TRUE)
                    .callbackUrl(callbackUrl)
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
            log.warn("基金刷新任务下发失败 taskId={} fundCodeCount={}", taskEntity.getServerTaskId(), fundCodes.size());
            taskEntity.transitTo(ProcessingTaskStatusEnumVO.DISPATCH_FAILED, safeSummary(e.getMessage()));
        }
        processingTaskRepository.updateTask(taskEntity);
        return toTaskDTO(taskEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundRefreshTaskResult createAndDispatchStockQuotes() {
        List<StockQuoteTargetEntity> quoteTargets = stockMarketRepository.queryAllQuoteTargets();
        if (quoteTargets.isEmpty()) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "股票刷新范围为空");
        }

        ProcessingTaskEntity taskEntity = ProcessingTaskEntity.builder()
                .serverTaskId(newStockServerTaskId())
                .taskType(ProcessingTaskEntity.STOCK_QUOTE_REFRESH)
                .taskParamsJson(buildStockTaskParamsJson(quoteTargets.size()))
                .status(ProcessingTaskStatusEnumVO.CREATED)
                .build();
        processingTaskRepository.saveTask(taskEntity);

        try {
            FundRefreshDispatchResultEntity dispatchResult = agentStockQuoteRefreshPort.dispatch(StockQuoteRefreshDispatchCommandEntity.builder()
                    .schemaVersion(STOCK_TASK_SCHEMA_VERSION)
                    .serverTaskId(taskEntity.getServerTaskId())
                    .stocks(quoteTargets)
                    .allowNetwork(Boolean.TRUE)
                    .callbackUrl(stockCallbackUrl)
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
            log.warn("股票行情刷新任务下发失败 taskId={} stockCount={}", taskEntity.getServerTaskId(), quoteTargets.size());
            taskEntity.transitTo(ProcessingTaskStatusEnumVO.DISPATCH_FAILED, safeSummary(e.getMessage()));
        }
        processingTaskRepository.updateTask(taskEntity);
        return toTaskDTO(taskEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundRefreshTaskResult handleCallback(AgentFundRefreshCallbackCommand command) {
        if (command == null || isBlank(command.getServerTaskId())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调缺少任务标识");
        }

        ProcessingTaskEntity taskEntity = processingTaskRepository.queryTask(command.getServerTaskId());
        if (taskEntity == null || !ProcessingTaskEntity.FUND_DETAIL_REFRESH.equals(taskEntity.getTaskType())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未知基金刷新任务");
        }

        if (!RESULT_SCHEMA_VERSION.equals(command.getSchemaVersion())) {
            taskEntity.transitTo(ProcessingTaskStatusEnumVO.FAILED, "unsupported schema version");
            processingTaskRepository.updateTask(taskEntity);
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "不支持的回调契约版本");
        }

        if (isBlank(command.getIdempotencyKey())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调缺少幂等键");
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
        try {
            if (targetStatus == ProcessingTaskStatusEnumVO.SUCCEEDED
                    || targetStatus == ProcessingTaskStatusEnumVO.PARTIAL_FAILED) {
                FundCurrentDataAggregate aggregate = toAggregate(command);
                fundDataRepository.saveCurrentData(aggregate);
                stockMarketRepository.registerQuoteTargets(toQuoteTargets(aggregate));
            }
            taskEntity.transitTo(targetStatus, safeSummary(command.getErrorSummary()));
            processingTaskRepository.updateTask(taskEntity);
            processingTaskRepository.markCallbackProcessed(command.getServerTaskId(), command.getIdempotencyKey(), "processed", null);
            return toTaskDTO(taskEntity);
        } catch (Exception e) {
            processingTaskRepository.markCallbackProcessed(command.getServerTaskId(), command.getIdempotencyKey(), "failed", safeSummary(e.getMessage()));
            throw e;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundRefreshTaskResult handleStockQuoteCallback(AgentStockQuoteRefreshCallbackCommand command) {
        if (command == null || isBlank(command.getServerTaskId())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调缺少任务标识");
        }

        ProcessingTaskEntity taskEntity = processingTaskRepository.queryTask(command.getServerTaskId());
        if (taskEntity == null || !ProcessingTaskEntity.STOCK_QUOTE_REFRESH.equals(taskEntity.getTaskType())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未知股票行情刷新任务");
        }

        if (!STOCK_RESULT_SCHEMA_VERSION.equals(command.getSchemaVersion())) {
            taskEntity.transitTo(ProcessingTaskStatusEnumVO.FAILED, "unsupported schema version");
            processingTaskRepository.updateTask(taskEntity);
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "不支持的回调契约版本");
        }

        if (isBlank(command.getIdempotencyKey())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调缺少幂等键");
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
        try {
            if (targetStatus == ProcessingTaskStatusEnumVO.SUCCEEDED
                    || targetStatus == ProcessingTaskStatusEnumVO.PARTIAL_FAILED) {
                stockMarketRepository.upsertQuotes(toStockQuotes(command.getQuotes()));
                processingTaskRepository.saveLogs(toStockWarnings(command.getServerTaskId(), command.getRefreshWarnings()));
            }
            taskEntity.transitTo(targetStatus, safeSummary(command.getErrorSummary()));
            processingTaskRepository.updateTask(taskEntity);
            processingTaskRepository.markCallbackProcessed(command.getServerTaskId(), command.getIdempotencyKey(), "processed", null);
            return toTaskDTO(taskEntity);
        } catch (Exception e) {
            processingTaskRepository.markCallbackProcessed(command.getServerTaskId(), command.getIdempotencyKey(), "failed", safeSummary(e.getMessage()));
            throw e;
        }
    }

    @Override
    public FundRefreshTaskResult queryTask(String serverTaskId) {
        ProcessingTaskEntity taskEntity = processingTaskRepository.queryTask(serverTaskId);
        if (taskEntity == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "任务不存在");
        }
        return toTaskDTO(taskEntity);
    }

    private FundCurrentDataAggregate toAggregate(AgentFundRefreshCallbackCommand request) {
        return FundCurrentDataAggregate.builder()
                .schemaVersion(request.getSchemaVersion())
                .generatedAt(parseInstantDate(request.getGeneratedAt()))
                .status(request.getStatus())
                .sourceRefId(request.getServerTaskId())
                .funds(toFundDetails(request.getFunds()))
                .warnings(toWarnings(request.getRefreshWarnings()))
                .build();
    }

    private List<FundCurrentDataAggregate.FundDetail> toFundDetails(List<AgentFundRefreshCallbackCommand.FundDetail> funds) {
        if (funds == null) {
            return List.of();
        }
        List<FundCurrentDataAggregate.FundDetail> result = new ArrayList<>();
        for (AgentFundRefreshCallbackCommand.FundDetail fund : funds) {
            if (fund == null || isBlank(fund.getFundCode())) {
                continue;
            }
            result.add(FundCurrentDataAggregate.FundDetail.builder()
                    .fundCode(fund.getFundCode().trim())
                    .fundName(defaultString(fund.getFundName(), fund.getFundCode().trim()))
                    .buyStatus(defaultString(fund.getBuyStatus(), "unknown"))
                    .dailyPurchaseLimit(fund.getDailyPurchaseLimit())
                    .returnsAsOf(parseLocalDate(fund.getReturnsAsOf()))
                    .topHoldingsAsOf(parseLocalDate(fund.getTopHoldingsAsOf()))
                    .publicHoldingsStatus(defaultString(fund.getPublicHoldingsStatus(), "missing"))
                    .oneMonthReturn(fund.getOneMonthReturn())
                    .threeMonthsReturn(fund.getThreeMonthsReturn())
                    .sixMonthsReturn(fund.getSixMonthsReturn())
                    .oneYearReturn(fund.getOneYearReturn())
                    .threeYearsReturn(fund.getThreeYearsReturn())
                    .topHoldings(toTopHoldings(fund.getFundCode().trim(), fund.getTopHoldings()))
                    .build());
        }
        return result;
    }

    private List<FundCurrentDataAggregate.TopHolding> toTopHoldings(String fundCode, List<AgentFundRefreshCallbackCommand.TopHolding> topHoldings) {
        if (topHoldings == null) {
            return List.of();
        }
        List<FundCurrentDataAggregate.TopHolding> result = new ArrayList<>();
        for (AgentFundRefreshCallbackCommand.TopHolding topHolding : topHoldings) {
            if (topHolding == null || topHolding.getRankNo() == null) {
                continue;
            }
            result.add(FundCurrentDataAggregate.TopHolding.builder()
                    .fundCode(fundCode)
                    .rankNo(topHolding.getRankNo())
                    .stockName(topHolding.getStockName())
                    .stockCode(topHolding.getStockCode())
                    .market(topHolding.getMarket())
                    .holdingRatio(topHolding.getHoldingRatio())
                    .quarterChangeType(defaultString(topHolding.getQuarterChangeType(), "unknown"))
                    .quarterChangeValue(topHolding.getQuarterChangeValue())
                    .build());
        }
        return result;
    }

    private List<StockQuoteEntity> toQuoteTargets(FundCurrentDataAggregate aggregate) {
        if (aggregate == null || aggregate.getFunds() == null) {
            return List.of();
        }
        Map<String, StockQuoteEntity> dedup = new LinkedHashMap<>();
        for (FundCurrentDataAggregate.FundDetail fund : aggregate.getFunds()) {
            if (fund == null || fund.getTopHoldings() == null) {
                continue;
            }
            for (FundCurrentDataAggregate.TopHolding topHolding : fund.getTopHoldings()) {
                if (topHolding == null || isBlank(topHolding.getStockCode())) {
                    continue;
                }
                String stockCode = topHolding.getStockCode().trim();
                String market = normalizeNullable(topHolding.getMarket());
                dedup.putIfAbsent(stockKey(stockCode, market), StockQuoteEntity.builder()
                        .stockCode(stockCode)
                        .market(market)
                        .stockName(isBlank(topHolding.getStockName()) ? null : topHolding.getStockName().trim())
                        .build());
            }
        }
        return new ArrayList<>(dedup.values());
    }

    private List<FundCurrentDataAggregate.RefreshWarning> toWarnings(List<AgentFundRefreshCallbackCommand.RefreshWarning> warnings) {
        if (warnings == null) {
            return List.of();
        }
        return warnings.stream()
                .filter(Objects::nonNull)
                .filter(warning -> !isBlank(warning.getModule()) && !isBlank(warning.getEvent()))
                .map(warning -> FundCurrentDataAggregate.RefreshWarning.builder()
                        .module(warning.getModule().trim())
                        .event(warning.getEvent().trim())
                        .message(safeSummary(defaultString(warning.getMessage(), warning.getEvent())))
                        .severity(defaultString(warning.getSeverity(), "warning"))
                        .build())
                .toList();
    }

    private List<StockQuoteEntity> toStockQuotes(List<AgentStockQuoteRefreshCallbackCommand.StockQuote> quotes) {
        if (quotes == null) {
            return List.of();
        }
        List<StockQuoteEntity> result = new ArrayList<>();
        for (AgentStockQuoteRefreshCallbackCommand.StockQuote quote : quotes) {
            if (quote == null || isBlank(quote.getStockCode())) {
                continue;
            }
            result.add(StockQuoteEntity.builder()
                    .stockCode(quote.getStockCode().trim())
                    .market(normalizeNullable(quote.getMarket()))
                    .stockName(quote.getStockName())
                    .tradeDate(parseLocalDate(quote.getTradeDate()))
                    .dailyReturn(quote.getDailyReturn())
                    .quoteTime(parseInstantDateOrNull(quote.getQuoteTime()))
                    .build());
        }
        return result;
    }

    private List<ProcessingLogEntity> toStockWarnings(String serverTaskId,
                                                      List<AgentStockQuoteRefreshCallbackCommand.RefreshWarning> warnings) {
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

    private List<String> normalizeFundCodes(List<String> fundCodes) {
        if (fundCodes == null) {
            return List.of();
        }
        Set<String> dedup = new LinkedHashSet<>();
        for (String fundCode : fundCodes) {
            if (!isBlank(fundCode)) {
                dedup.add(fundCode.trim());
            }
        }
        return new ArrayList<>(dedup);
    }

    private java.util.Date parseInstantDate(String value) {
        if (isBlank(value)) {
            return new java.util.Date();
        }
        try {
            return java.util.Date.from(Instant.parse(value));
        } catch (DateTimeParseException e) {
            return new java.util.Date();
        }
    }

    private java.util.Date parseInstantDateOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return java.util.Date.from(Instant.parse(value));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private java.util.Date parseLocalDate(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Date.valueOf(LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String newServerTaskId() {
        return "fund_refresh_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String newStockServerTaskId() {
        return "stock_quote_refresh_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildTaskParamsJson(int fundCodeCount) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fundCodeCount", fundCodeCount);
        params.put("trigger", "system");
        return JSON.toJSONString(params);
    }

    private String buildStockTaskParamsJson(int stockCount) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("stockCount", stockCount);
        params.put("trigger", "system");
        return JSON.toJSONString(params);
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

    private String stockKey(String stockCode, String market) {
        return stockCode + "#" + (market == null ? "" : market);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
