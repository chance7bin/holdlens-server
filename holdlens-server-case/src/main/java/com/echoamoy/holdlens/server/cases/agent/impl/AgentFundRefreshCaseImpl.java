package com.echoamoy.holdlens.server.cases.agent.impl;

import com.alibaba.fastjson.JSON;
import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.AgentFundRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.funddata.model.aggregate.FundCurrentDataAggregate;
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
import com.echoamoy.holdlens.server.types.common.DateTimeUtils;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private IAgentFundRefreshPort agentFundRefreshPort;

    @Resource
    private IAgentAShareMarketRefreshPort agentAShareMarketRefreshPort;

    @Resource
    private IAgentUSStockMarketRefreshPort agentUSStockMarketRefreshPort;

    @Resource
    private IFundDataRepository fundDataRepository;

    @Resource
    private IStockMarketRepository stockMarketRepository;

    @Value("${holdlens.agent.callback-url:http://127.0.0.1:8091/internal/agent/fund-detail-refresh/callback}")
    private String callbackUrl;

    @Value("${holdlens.agent.a-share-market-callback-url:http://127.0.0.1:8091/internal/agent/a-share-market-refresh/callback}")
    private String aShareMarketCallbackUrl;

    @Value("${holdlens.agent.us-stock-market-callback-url:http://127.0.0.1:8091/internal/agent/us-stock-market-refresh/callback}")
    private String usStockMarketCallbackUrl;

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
                .taskParamsJson(buildTaskParamsJson(fundCodes.size(), command == null ? null : command.getTrigger()))
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
    public boolean hasNonTerminalTask(String taskType) {
        if (isBlank(taskType)) {
            return false;
        }
        return processingTaskRepository.existsNonTerminalTask(taskType.trim());
    }

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
    public FundRefreshTaskResult handleAShareMarketCallback(AShareMarketRefreshCallbackCommand command) {
        if (command == null || isBlank(command.getServerTaskId())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调缺少任务标识");
        }

        ProcessingTaskEntity taskEntity = processingTaskRepository.queryTask(command.getServerTaskId());
        if (taskEntity == null || !ProcessingTaskEntity.A_SHARE_MARKET_REFRESH.equals(taskEntity.getTaskType())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未知 A 股全量行情刷新任务");
        }

        if (!A_SHARE_MARKET_RESULT_SCHEMA_VERSION.equals(command.getSchemaVersion())) {
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
                List<ProcessingLogEntity> diagnostics = new ArrayList<>(toAShareMarketWarnings(command.getServerTaskId(), command.getRefreshWarnings()));
                List<StockMarketEntity> markets = toStockMarkets(command, diagnostics);
                upsertStockMarkets(markets);
                processingTaskRepository.saveLogs(diagnostics);
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
    public FundRefreshTaskResult handleUSStockMarketCallback(USStockMarketRefreshCallbackCommand command) {
        if (command == null || isBlank(command.getServerTaskId())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调缺少任务标识");
        }

        ProcessingTaskEntity taskEntity = processingTaskRepository.queryTask(command.getServerTaskId());
        if (taskEntity == null || !ProcessingTaskEntity.US_STOCK_MARKET_REFRESH.equals(taskEntity.getTaskType())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未知美股全量行情刷新任务");
        }

        if (!US_STOCK_MARKET_RESULT_SCHEMA_VERSION.equals(command.getSchemaVersion())) {
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
                List<ProcessingLogEntity> diagnostics = new ArrayList<>(toUSStockMarketWarnings(command.getServerTaskId(), command.getRefreshWarnings()));
                List<StockMarketEntity> markets = toUSStockMarkets(command, diagnostics);
                upsertStockMarkets(markets);
                processingTaskRepository.saveLogs(diagnostics);
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
                .generatedAt(DateTimeUtils.parseOffsetDateTimeOrNow(request.getGeneratedAt()))
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
                    .fundName(normalizeNullable(fund.getFundName()))
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

    private List<StockMarketEntity> toQuoteTargets(FundCurrentDataAggregate aggregate) {
        if (aggregate == null || aggregate.getFunds() == null) {
            return List.of();
        }
        Map<String, StockMarketEntity> dedup = new LinkedHashMap<>();
        for (FundCurrentDataAggregate.FundDetail fund : aggregate.getFunds()) {
            if (fund == null || fund.getTopHoldings() == null) {
                continue;
            }
            for (FundCurrentDataAggregate.TopHolding topHolding : fund.getTopHoldings()) {
                if (topHolding == null || isBlank(topHolding.getStockCode())) {
                    continue;
                }
                String stockCode = topHolding.getStockCode().trim();
                String market = StockMarketEntity.MARKET_A_SHARE;
                dedup.putIfAbsent(stockKey(stockCode, market), StockMarketEntity.builder()
                        .stockCode(stockCode)
                        .market(market)
                        .stockName(isBlank(topHolding.getStockName()) ? null : topHolding.getStockName().trim())
                        .currency(StockMarketEntity.CURRENCY_CNY)
                        .volumeUnit(StockMarketEntity.VOLUME_UNIT_LOT)
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

    private String newAShareMarketServerTaskId() {
        return "a_share_market_refresh_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String newUSStockMarketServerTaskId() {
        return "us_stock_market_refresh_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildTaskParamsJson(int fundCodeCount, String trigger) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fundCodeCount", fundCodeCount);
        params.put("trigger", defaultString(trigger, "system"));
        return JSON.toJSONString(params);
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

    private String stockKey(String stockCode, String market) {
        return stockCode + "#" + (market == null ? "" : market);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
