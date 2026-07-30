package com.echoamoy.holdlens.server.cases.marketdetail.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.echoamoy.holdlens.server.cases.marketdetail.IMarketDetailCase;
import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailCommand;
import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailResult;
import com.echoamoy.holdlens.server.cases.support.TransactionExecutor;
import com.echoamoy.holdlens.server.domain.funddata.adapter.repository.IFundDataRepository;
import com.echoamoy.holdlens.server.domain.marketasset.model.valobj.MarketAssetRefVO;
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
import com.echoamoy.holdlens.server.domain.processing.model.valobj.ProcessingTaskStatusEnumVO;
import com.echoamoy.holdlens.server.domain.stockdata.adapter.repository.IStockMarketRepository;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import com.echoamoy.holdlens.server.types.common.DateTimeUtils;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class MarketDetailCaseImpl implements IMarketDetailCase {

    private static final String TASK_SCHEMA = "market-detail-data-refresh-task/v1";
    private static final String RESULT_SCHEMA = "market-detail-data-refresh-result/v1";
    private static final int MAX_FUND_NAV_POINTS = 10000;
    private static final Set<String> STOCK_PERIODS = Set.of("intraday", "5d", "1m", "1y");
    private static final Set<String> FUND_PERIODS = Set.of("1m", "3m", "1y", "all");
    private static final List<String> FUND_DETAIL_SLICES = List.of("nav_history", "period_performance");
    private static final List<String> PERFORMANCE_PERIODS = List.of("1m", "3m", "6m", "1y", "3y");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Pattern SENSITIVE_TEXT_PATTERN = Pattern.compile(
            "(?i)\\b(api[_-]?key|authorization|callback[_-]?auth|cookie|password|secret|token)\\b\\s*[:=]\\s*[^,;]+");

    @Resource private IProcessingTaskRepository processingTaskRepository;
    @Resource private IFundDataRepository fundDataRepository;
    @Resource private IStockMarketRepository stockMarketRepository;
    @Resource private IMarketDetailRepository marketDetailRepository;
    @Resource private IAgentMarketDetailRefreshPort agentMarketDetailRefreshPort;
    @Resource private TransactionExecutor transactionExecutor;

    @Value("${holdlens.agent.market-detail-data-refresh-callback-url}")
    private String callbackUrl;
    @Value("${holdlens.agent.market-detail-data-refresh.cooldown-minutes:10}")
    private long refreshCooldownMinutes;
    @Value("${holdlens.agent.market-detail-data-refresh.timeout-minutes:10}")
    private long refreshTimeoutMinutes;

    @Override
    public MarketDetailResult.Task createAndDispatch(MarketDetailCommand.CreateTask command) {
        TaskPlan plan = validateCreate(command);
        ProcessingTaskEntity task = ProcessingTaskEntity.builder()
                .serverTaskId("market_detail_data_refresh_" + UUID.randomUUID().toString().replace("-", ""))
                .taskType(ProcessingTaskEntity.MARKET_DETAIL_DATA_REFRESH)
                .taskParamsJson(taskParams(plan)).status(ProcessingTaskStatusEnumVO.CREATED).build();
        processingTaskRepository.saveTask(task);
        dispatchTask(task, plan);
        processingTaskRepository.updateTask(task);
        return toTask(task);
    }

    @Override
    public MarketDetailResult.Task handleCallback(MarketDetailCommand.Callback command) {
        CallbackPlan plan = validateCallback(command);
        boolean first = processingTaskRepository.saveCallbackIfAbsent(ProcessingCallbackEntity.builder()
                .serverTaskId(command.getServerTaskId()).idempotencyKey(command.getIdempotencyKey())
                .callbackStatus(command.getStatus()).processStatus("processing")
                .errorSummary(safe(command.getErrorSummary(), 500)).build());
        if (!first || plan.task.isTerminal()) {
            return toTask(processingTaskRepository.queryTask(command.getServerTaskId()));
        }

        int successfulSlices = 0;
        int failedSlices = 0;
        if (!"failed".equals(command.getStatus())) {
            if (plan.slices.contains("nav_history")) {
                if (command.getFundNavHistory() == null) {
                    if ("partial_failed".equals(command.getStatus())) {
                        failedSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "nav_history", "failed", command.getErrorSummary());
                    } else {
                        successfulSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "nav_history", "empty", null);
                    }
                } else {
                    try {
                        List<FundNavHistoryEntity> points = toFundNav(plan.ref, command);
                        transactionExecutor.requiresNew(() -> {
                            marketDetailRepository.upsertFundNavHistory(points);
                            updateFundSliceState(command, plan.ref.getAssetCode(), "nav_history",
                                    points.isEmpty() ? "empty" : "available", null);
                            return null;
                        });
                        successfulSlices++;
                    } catch (RuntimeException exception) {
                        failedSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "nav_history", "failed", "nav history rejected");
                        recordSlicePersistenceFailure(command.getServerTaskId(), "nav_history", exception);
                    }
                }
            }
            if (plan.slices.contains("period_performance")) {
                if (command.getFundPeriodPerformance() == null) {
                    if ("partial_failed".equals(command.getStatus())) {
                        failedSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "period_performance", "failed", command.getErrorSummary());
                    } else {
                        successfulSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "period_performance", "empty", null);
                    }
                } else {
                    try {
                        List<FundPeriodPerformanceEntity> rows = toFundPeriodPerformance(plan.ref, command);
                        transactionExecutor.requiresNew(() -> {
                            marketDetailRepository.upsertFundPeriodPerformance(rows);
                            updateFundSliceState(command, plan.ref.getAssetCode(), "period_performance",
                                    rows.isEmpty() ? "empty" : "available", null);
                            return null;
                        });
                        successfulSlices++;
                    } catch (RuntimeException exception) {
                        failedSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "period_performance", "failed",
                                "period performance rejected");
                        recordSlicePersistenceFailure(command.getServerTaskId(), "period_performance", exception);
                    }
                }
            }
            if (plan.slices.contains("price_history")) {
                if (command.getStockPriceHistories() == null) {
                    if ("partial_failed".equals(command.getStatus())) failedSlices++;
                    else successfulSlices++;
                } else {
                    try {
                        List<StockPriceBarEntity> bars = toStockBars(plan, command);
                        transactionExecutor.requiresNew(() -> { marketDetailRepository.upsertStockPriceBars(bars); return null; });
                        successfulSlices++;
                    } catch (RuntimeException exception) {
                        failedSlices++;
                        recordSlicePersistenceFailure(command.getServerTaskId(), "price_history", exception);
                    }
                }
            }
            if (plan.slices.contains("company_profile")) {
                if (command.getStockCompanyProfile() == null) {
                    if ("partial_failed".equals(command.getStatus())) failedSlices++;
                    else successfulSlices++;
                } else {
                    try {
                        StockCompanyProfileEntity profile = toProfile(plan.ref, command);
                        transactionExecutor.requiresNew(() -> { marketDetailRepository.upsertStockCompanyProfile(profile); return null; });
                        successfulSlices++;
                    } catch (RuntimeException exception) {
                        failedSlices++;
                        recordSlicePersistenceFailure(command.getServerTaskId(), "company_profile", exception);
                    }
                }
            }
        }
        if ("failed".equals(command.getStatus()) && MarketAssetRefVO.KIND_FUND.equals(plan.ref.getAssetKind())) {
            for (String slice : plan.slices) {
                convergeFundSlice(command, plan.ref.getAssetCode(), slice, "failed", command.getErrorSummary());
                failedSlices++;
            }
        }

        ProcessingTaskStatusEnumVO target = callbackTarget(command.getStatus(), successfulSlices, failedSlices);
        ProcessingTaskEntity latest = processingTaskRepository.queryTask(command.getServerTaskId());
        if (!latest.isTerminal()) {
            latest.transitTo(target, safe(command.getErrorSummary(), 500));
            processingTaskRepository.updateTask(latest);
        }
        saveWarnings(command);
        processingTaskRepository.markCallbackProcessed(command.getServerTaskId(), command.getIdempotencyKey(),
                "processed", failedSlices == 0 ? null : "one or more slices rejected");
        return toTask(latest);
    }

    @Override
    public MarketDetailResult.FundNavHistory queryFundNavHistory(String fundCode, String period) {
        if (fundCode == null || fundCode.isBlank() || !FUND_PERIODS.contains(period)) throw illegal("基金历史参数不合法");
        String code = fundCode.trim();
        if (!fundDataRepository.queryExistingFundCodes(List.of(code)).contains(code)) throw illegal("基金不存在");
        LocalDate latest = marketDetailRepository.queryLatestFundNavDate(code);
        LocalDate start = latest == null || "all".equals(period) ? null : switch (period) {
            case "1m" -> latest.minusMonths(1); case "3m" -> latest.minusMonths(3); case "1y" -> latest.minusYears(1);
            default -> null;
        };
        List<MarketDetailResult.FundNavPoint> points = marketDetailRepository.queryFundNavHistory(code, start).stream()
                .map(p -> MarketDetailResult.FundNavPoint.builder().navDate(p.getNavDate().toString()).unitNav(p.getUnitNav())
                        .accumulatedNav(p.getAccumulatedNav()).dailyGrowthRate(p.getDailyGrowthRate()).build()).toList();
        return MarketDetailResult.FundNavHistory.builder().fundCode(code).period(period)
                .asOf(latest == null ? null : latest.toString()).points(points).build();
    }

    @Override
    public MarketDetailResult.FundPeriodPerformance queryFundPeriodPerformance(String fundCode) {
        String code = requireFund(fundCode);
        List<FundPeriodPerformanceEntity> entities = latestPerformanceSnapshot(code);
        List<MarketDetailResult.FundPeriodPerformanceRow> rows = entities.stream()
                .sorted(Comparator.comparingInt(row -> PERFORMANCE_PERIODS.indexOf(row.getPeriod())))
                .map(row -> MarketDetailResult.FundPeriodPerformanceRow.builder().period(row.getPeriod())
                        .fundReturn(row.getFundReturn()).peerAverage(row.getPeerAverage()).peerRank(row.getPeerRank())
                        .peerTotal(row.getPeerTotal()).rankChange(row.getRankChange()).build()).toList();
        LocalDate asOf = entities.stream().map(FundPeriodPerformanceEntity::getAsOf)
                .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        return MarketDetailResult.FundPeriodPerformance.builder().fundCode(code)
                .asOf(asOf == null ? null : asOf.toString()).rows(rows).build();
    }

    @Override
    public MarketDetailResult.FundDetailRefresh requestFundDetailRefresh(String fundCode) {
        String code = requireFund(fundCode);
        FundRefreshClaim claim = transactionExecutor.required(() -> claimFundDetailRefresh(code));
        if (claim.task != null) {
            dispatchTask(claim.task, claim.plan);
            ProcessingTaskStatusEnumVO dispatchStatus = claim.task.getStatus();
            String dispatchError = claim.task.getErrorSummary();
            ProcessingTaskEntity persisted = transactionExecutor.required(() -> {
                ProcessingTaskEntity latest = processingTaskRepository.queryTaskForUpdate(claim.task.getServerTaskId());
                if (latest != null && !latest.isTerminal()) {
                    latest.transitTo(dispatchStatus, dispatchError);
                    processingTaskRepository.updateTask(latest);
                }
                return latest;
            });
            if (persisted != null && persisted.getStatus() == ProcessingTaskStatusEnumVO.DISPATCH_FAILED) {
                transactionExecutor.required(() -> {
                    for (String slice : claim.plan.slices) {
                        MarketDetailSliceStateEntity failed = MarketDetailSliceStateEntity.builder()
                                .fundCode(code).sliceType(slice).status("failed")
                                .activeTaskId(claim.task.getServerTaskId()).lastAttemptAt(DateTimeUtils.now())
                                .errorSummary("agent dispatch failed").build();
                        marketDetailRepository.updateFundSliceStateIfActiveTask(failed);
                    }
                    return null;
                });
                claim.statuses.replaceAll((slice, status) -> claim.plan.slices.contains(slice) ? "failed" : status);
            }
        }
        return toFundDetailRefresh(code, claim.statuses);
    }

    @Override
    public MarketDetailResult.StockPriceHistory queryStockPriceHistory(String assetRef, String period) {
        if (!STOCK_PERIODS.contains(period)) throw illegal("股票历史 period 不合法");
        MarketAssetRefVO ref = parseStockRef(assetRef);
        StockMarketEntity stock = requireStock(ref);
        String granularity = "intraday".equals(period) ? "minute" : "day";
        LocalDateTime latest = marketDetailRepository.queryLatestStockBarTime(ref.getAssetCode(), ref.getMarket(), granularity);
        LocalDateTime start = latest == null ? null : switch (period) {
            case "intraday" -> latest.toLocalDate().atStartOfDay(); case "5d" -> latest.minusMonths(1);
            case "1m" -> latest.minusMonths(1); case "1y" -> latest.minusYears(1); default -> null;
        };
        List<MarketDetailResult.StockBar> points = marketDetailRepository
                .queryStockPriceBars(ref.getAssetCode(), ref.getMarket(), granularity, start).stream()
                .map(b -> MarketDetailResult.StockBar.builder().barTime(format(b.getBarTime())).open(b.getOpen())
                        .high(b.getHigh()).low(b.getLow()).close(b.getClose()).volume(b.getVolume()).build()).toList();
        if ("5d".equals(period) && points.size() > 5) {
            points = List.copyOf(points.subList(points.size() - 5, points.size()));
        }
        return MarketDetailResult.StockPriceHistory.builder().assetRef(ref.value()).period(period)
                .granularity(granularity).currency(stock.getCurrency()).asOf(format(latest)).points(points).build();
    }

    @Override
    public MarketDetailResult.StockCompanyProfile queryStockCompanyProfile(String assetRef) {
        MarketAssetRefVO ref = parseStockRef(assetRef);
        requireStock(ref);
        StockCompanyProfileEntity p = marketDetailRepository.queryStockCompanyProfile(ref.getAssetCode(), ref.getMarket());
        return MarketDetailResult.StockCompanyProfile.builder().assetRef(ref.value())
                .companyName(p == null ? null : p.getCompanyName()).industry(p == null ? null : p.getIndustry())
                .businessSummary(p == null ? null : p.getBusinessSummary()).companyProfile(p == null ? null : p.getCompanyProfile())
                .website(p == null ? null : p.getWebsite()).asOf(p == null ? null : format(p.getSourceAsOf())).build();
    }

    private TaskPlan validateCreate(MarketDetailCommand.CreateTask command) {
        if (command == null) throw illegal("任务请求不能为空");
        MarketAssetRefVO ref;
        try { ref = MarketAssetRefVO.parse(command.getAssetKind(), command.getAssetRef()); }
        catch (IllegalArgumentException e) { throw illegal(e.getMessage()); }
        List<String> slices = dedup(command.getSlices());
        List<String> periods = dedup(command.getPeriods());
        if (slices.isEmpty()) throw illegal("slices 不能为空");
        String providerCode = null;
        if (MarketAssetRefVO.KIND_FUND.equals(ref.getAssetKind())) {
            if (slices.stream().anyMatch(slice -> !FUND_DETAIL_SLICES.contains(slice)) || !periods.isEmpty()) {
                throw illegal("基金 slices 不支持或 periods 必须为空");
            }
            if (!fundDataRepository.queryExistingFundCodes(List.of(ref.getAssetCode())).contains(ref.getAssetCode())) throw illegal("基金不存在");
        } else {
            if (slices.stream().anyMatch(s -> !Set.of("price_history", "company_profile").contains(s))) throw illegal("股票 slice 不支持");
            if (slices.contains("price_history") && (periods.isEmpty() || periods.stream().anyMatch(p -> !STOCK_PERIODS.contains(p)))) {
                throw illegal("股票价格历史 periods 不合法");
            }
            if (!slices.contains("price_history") && !periods.isEmpty()) {
                throw illegal("未请求 price_history 时 periods 必须为空");
            }
            StockMarketEntity stock = requireStock(ref);
            if (MarketAssetRefVO.MARKET_US_STOCK.equals(ref.getMarket()) && slices.contains("price_history")) {
                providerCode = stock.getProviderMarketCode();
                if (providerCode == null || providerCode.isBlank()) throw illegal("美股缺少 provider_market_code");
            }
        }
        return new TaskPlan(ref, slices, periods, providerCode);
    }

    private CallbackPlan validateCallback(MarketDetailCommand.Callback c) {
        if (c == null || c.getServerTaskId() == null) throw illegal("回调缺少任务标识");
        ProcessingTaskEntity task = processingTaskRepository.queryTask(c.getServerTaskId());
        if (task == null || !ProcessingTaskEntity.MARKET_DETAIL_DATA_REFRESH.equals(task.getTaskType())) throw illegal("未知任务");
        if (!RESULT_SCHEMA.equals(c.getSchemaVersion())) throw illegal("回调 schema 不支持");
        if (!("succeeded".equals(c.getStatus()) || "partial_failed".equals(c.getStatus()) || "failed".equals(c.getStatus()))) {
            throw illegal("回调状态不支持");
        }
        if (!(c.getServerTaskId() + ":result:1").equals(c.getIdempotencyKey())) throw illegal("幂等键不合法");
        JSONObject params = JSON.parseObject(task.getTaskParamsJson());
        MarketAssetRefVO ref;
        try { ref = MarketAssetRefVO.parse(params.getString("assetKind"), params.getString("assetRef")); }
        catch (IllegalArgumentException e) { throw illegal("任务引用不合法"); }
        if (!ref.value().equals(c.getAssetRef()) || !ref.getAssetKind().equals(c.getAssetKind())) throw illegal("回调资产与任务不一致");
        List<String> slices = params.getJSONArray("slices").toJavaList(String.class);
        if (c.getFundNavHistory() != null && c.getFundNavHistory().getPoints() != null
                && !c.getFundNavHistory().getPoints().isEmpty() && !slices.contains("nav_history")
                || c.getFundPeriodPerformance() != null && c.getFundPeriodPerformance().getRows() != null
                && !c.getFundPeriodPerformance().getRows().isEmpty() && !slices.contains("period_performance")
                || c.getStockPriceHistories() != null && !c.getStockPriceHistories().isEmpty()
                        && !slices.contains("price_history")
                || c.getStockCompanyProfile() != null && !slices.contains("company_profile")) {
            throw illegal("回调包含任务未请求的 slice");
        }
        return new CallbackPlan(task, ref, slices, params.getJSONArray("periods").toJavaList(String.class));
    }

    private FundRefreshClaim claimFundDetailRefresh(String fundCode) {
        LocalDateTime now = DateTimeUtils.now();
        marketDetailRepository.ensureFundSliceStates(fundCode, FUND_DETAIL_SLICES);
        Map<String, MarketDetailSliceStateEntity> states = new LinkedHashMap<>();
        for (String slice : FUND_DETAIL_SLICES) {
            states.put(slice, marketDetailRepository.lockFundSliceState(fundCode, slice));
        }
        Map<String, String> statuses = new LinkedHashMap<>();
        List<String> claimable = new ArrayList<>();
        boolean hasNav = marketDetailRepository.queryLatestFundNavDate(fundCode) != null;
        boolean hasPerformance = !latestPerformanceSnapshot(fundCode).isEmpty();
        for (String slice : FUND_DETAIL_SLICES) {
            MarketDetailSliceStateEntity state = states.get(slice);
            boolean factExists = "nav_history".equals(slice) ? hasNav : hasPerformance;
            if (factExists) {
                state.setStatus("available");
                state.setActiveTaskId(null);
                state.setLastSuccessAt(now);
                state.setErrorSummary(null);
                marketDetailRepository.updateFundSliceState(state);
                statuses.put(slice, "available");
                continue;
            }
            if ("refreshing".equals(state.getStatus()) && state.getActiveTaskId() != null) {
                ProcessingTaskEntity active = processingTaskRepository.queryTaskForUpdate(state.getActiveTaskId());
                boolean timedOut = state.getLastAttemptAt() == null
                        || state.getLastAttemptAt().isBefore(now.minusMinutes(refreshTimeoutMinutes));
                if (active != null && !active.isTerminal() && !timedOut) {
                    statuses.put(slice, "refreshing");
                    continue;
                }
                if (active != null && !active.isTerminal()) {
                    active.transitTo(ProcessingTaskStatusEnumVO.FAILED, "market detail refresh timed out");
                    processingTaskRepository.updateTask(active);
                }
                state.setStatus("failed");
                state.setActiveTaskId(null);
                state.setLastAttemptAt(now);
                state.setErrorSummary("market detail refresh timed out");
                marketDetailRepository.updateFundSliceState(state);
            }
            boolean cooling = state.getLastAttemptAt() != null
                    && state.getLastAttemptAt().isAfter(now.minusMinutes(refreshCooldownMinutes))
                    && Set.of("empty", "failed").contains(state.getStatus());
            if (cooling) {
                statuses.put(slice, state.getStatus());
            } else {
                claimable.add(slice);
            }
        }
        if (claimable.isEmpty()) return new FundRefreshClaim(null, null, statuses);
        MarketAssetRefVO ref = MarketAssetRefVO.parse(MarketAssetRefVO.KIND_FUND, "fund:" + fundCode);
        TaskPlan plan = new TaskPlan(ref, List.copyOf(claimable), List.of(), null);
        ProcessingTaskEntity task = ProcessingTaskEntity.builder()
                .serverTaskId("market_detail_data_refresh_" + UUID.randomUUID().toString().replace("-", ""))
                .taskType(ProcessingTaskEntity.MARKET_DETAIL_DATA_REFRESH).taskParamsJson(taskParams(plan))
                .status(ProcessingTaskStatusEnumVO.CREATED).build();
        processingTaskRepository.saveTask(task);
        for (String slice : claimable) {
            MarketDetailSliceStateEntity state = states.get(slice);
            state.setStatus("refreshing");
            state.setActiveTaskId(task.getServerTaskId());
            state.setLastAttemptAt(now);
            state.setErrorSummary(null);
            marketDetailRepository.updateFundSliceState(state);
            statuses.put(slice, "refreshing");
        }
        return new FundRefreshClaim(task, plan, statuses);
    }

    private MarketDetailResult.FundDetailRefresh toFundDetailRefresh(String fundCode, Map<String, String> statuses) {
        List<MarketDetailResult.FundDetailSlice> slices = FUND_DETAIL_SLICES.stream()
                .map(slice -> MarketDetailResult.FundDetailSlice.builder().slice(slice)
                        .status(statuses.getOrDefault(slice, "failed")).build()).toList();
        String overall = statuses.values().stream().allMatch("available"::equals) ? "ready"
                : statuses.values().stream().anyMatch("refreshing"::equals) ? "refreshing" : "unavailable";
        return MarketDetailResult.FundDetailRefresh.builder().fundCode(fundCode).status(overall)
                .retryAfterMs(1000L).slices(slices).build();
    }

    private void dispatchTask(ProcessingTaskEntity task, TaskPlan plan) {
        try {
            MarketDetailDispatchResultVO dispatched = agentMarketDetailRefreshPort.dispatch(
                    MarketDetailDispatchCommandEntity.builder().schemaVersion(TASK_SCHEMA)
                            .serverTaskId(task.getServerTaskId()).assetKind(plan.ref.getAssetKind())
                            .assetRef(plan.ref.value()).providerMarketCode(plan.providerMarketCode)
                            .slices(plan.slices).periods(plan.periods).callbackUrl(callbackUrl).allowNetwork(true)
                            .requestedAt(java.time.OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                            .build());
            if (dispatched != null && dispatched.isAccepted()) {
                task.transitTo(ProcessingTaskStatusEnumVO.DISPATCHED, null);
            } else {
                task.transitTo(ProcessingTaskStatusEnumVO.DISPATCH_FAILED,
                        safe(dispatched == null ? "agent dispatch rejected" : dispatched.getErrorSummary(), 500));
            }
        } catch (RuntimeException exception) {
            task.transitTo(ProcessingTaskStatusEnumVO.DISPATCH_FAILED, "agent dispatch failed");
        }
    }

    private List<FundNavHistoryEntity> toFundNav(MarketAssetRefVO ref, MarketDetailCommand.Callback c) {
        MarketDetailCommand.FundNavHistory h = c.getFundNavHistory();
        if (!ref.getAssetCode().equals(h.getFundCode())) throw illegal("基金净值代码不一致");
        List<MarketDetailCommand.FundNavPoint> input = h.getPoints() == null ? List.of() : h.getPoints();
        if (input.size() > MAX_FUND_NAV_POINTS) throw illegal("基金净值点过多");
        LocalDateTime fetched = DateTimeUtils.now();
        List<FundNavHistoryEntity> result = new ArrayList<>();
        Set<LocalDate> dates = new LinkedHashSet<>();
        for (MarketDetailCommand.FundNavPoint p : input) {
            if (p == null) throw illegal("基金净值点为空");
            LocalDate date = parseDate(p.getNavDate());
            if (!dates.add(date)) throw illegal("基金净值日期重复");
            result.add(FundNavHistoryEntity.builder().fundCode(ref.getAssetCode()).navDate(date)
                    .unitNav(requiredNonNegative(p.getUnitNav())).accumulatedNav(nonNegative(p.getAccumulatedNav()))
                    .dailyGrowthRate(decimal(p.getDailyGrowthRate())).sourceAsOf(parseTime(c.getGeneratedAt()))
                    .fetchedAt(fetched).build());
        }
        return result;
    }

    private List<FundPeriodPerformanceEntity> toFundPeriodPerformance(MarketAssetRefVO ref,
                                                                      MarketDetailCommand.Callback c) {
        MarketDetailCommand.FundPeriodPerformance performance = c.getFundPeriodPerformance();
        if (!ref.getAssetCode().equals(performance.getFundCode())) throw illegal("基金阶段业绩代码不一致");
        LocalDate asOf = parseDate(performance.getAsOf());
        List<MarketDetailCommand.FundPeriodPerformanceRow> input = performance.getRows() == null
                ? List.of() : performance.getRows();
        if (input.size() > PERFORMANCE_PERIODS.size()) throw illegal("基金阶段业绩行过多");
        Set<String> periods = new LinkedHashSet<>();
        LocalDateTime fetchedAt = DateTimeUtils.now();
        List<FundPeriodPerformanceEntity> rows = new ArrayList<>();
        for (MarketDetailCommand.FundPeriodPerformanceRow row : input) {
            if (row == null || !PERFORMANCE_PERIODS.contains(row.getPeriod()) || !periods.add(row.getPeriod())) {
                throw illegal("基金阶段业绩 period 不合法或重复");
            }
            if (row.getPeerRank() != null && row.getPeerRank() <= 0
                    || row.getPeerTotal() != null && row.getPeerTotal() <= 0
                    || row.getPeerRank() != null && row.getPeerTotal() != null
                    && row.getPeerRank() > row.getPeerTotal()) {
                throw illegal("基金阶段业绩排名不合法");
            }
            rows.add(FundPeriodPerformanceEntity.builder().fundCode(ref.getAssetCode()).period(row.getPeriod())
                    .fundReturn(decimal(row.getFundReturn())).peerAverage(decimal(row.getPeerAverage()))
                    .peerRank(row.getPeerRank()).peerTotal(row.getPeerTotal()).rankChange(row.getRankChange())
                    .asOf(asOf).fetchedAt(fetchedAt).build());
        }
        return rows;
    }

    private void convergeFundSlice(MarketDetailCommand.Callback command, String fundCode, String slice,
                                   String status, String errorSummary) {
        try {
            transactionExecutor.requiresNew(() -> {
                updateFundSliceState(command, fundCode, slice, status, errorSummary);
                return null;
            });
        } catch (RuntimeException exception) {
            recordSlicePersistenceFailure(command.getServerTaskId(), slice + "_state", exception);
        }
    }

    private void updateFundSliceState(MarketDetailCommand.Callback command, String fundCode, String slice,
                                      String status, String errorSummary) {
        LocalDateTime now = DateTimeUtils.now();
        marketDetailRepository.updateFundSliceStateIfActiveTask(MarketDetailSliceStateEntity.builder()
                .fundCode(fundCode).sliceType(slice).status(status).activeTaskId(command.getServerTaskId())
                .lastAttemptAt(now).lastSuccessAt("available".equals(status) ? now : null)
                .errorSummary(safe(errorSummary, 500)).build());
    }

    private List<StockPriceBarEntity> toStockBars(CallbackPlan plan, MarketDetailCommand.Callback c) {
        List<MarketDetailCommand.StockPriceHistory> histories = c.getStockPriceHistories();
        if (histories.size() > plan.periods.size()) throw illegal("价格历史 period 重复或超出任务范围");
        List<StockPriceBarEntity> result = new ArrayList<>();
        Set<String> identities = new LinkedHashSet<>();
        Set<String> seenPeriods = new LinkedHashSet<>();
        LocalDateTime fetched = DateTimeUtils.now();
        for (MarketDetailCommand.StockPriceHistory history : histories) {
            if (history == null || !plan.periods.contains(history.getPeriod()) || !seenPeriods.add(history.getPeriod())) throw illegal("价格历史 period 不一致");
            if (!Set.of("minute", "day").contains(history.getGranularity())) throw illegal("价格粒度不支持");
            if (("intraday".equals(history.getPeriod()) && !"minute".equals(history.getGranularity()))
                    || (Set.of("1m", "1y").contains(history.getPeriod()) && !"day".equals(history.getGranularity()))) {
                throw illegal("价格历史 period 与粒度不一致");
            }
            List<MarketDetailCommand.StockBar> bars = history.getBars() == null ? List.of() : history.getBars();
            if (bars.size() > 10000) throw illegal("价格 bar 过多");
            for (MarketDetailCommand.StockBar b : bars) {
                if (b == null) throw illegal("价格 bar 为空");
                LocalDateTime barTime = parseTimeRequired(b.getBarTime());
                String identity = history.getGranularity() + "#" + barTime;
                if (!identities.add(identity)) continue;
                result.add(StockPriceBarEntity.builder().stockCode(plan.ref.getAssetCode()).market(plan.ref.getMarket())
                        .granularity(history.getGranularity()).barTime(barTime).open(requiredNonNegative(b.getOpen())).high(requiredNonNegative(b.getHigh()))
                        .low(requiredNonNegative(b.getLow())).close(requiredNonNegative(b.getClose())).volume(nonNegative(b.getVolume()))
                        .currency(safe(history.getCurrency(), 3)).sourceAsOf(parseTime(c.getGeneratedAt())).fetchedAt(fetched).build());
            }
        }
        return result;
    }

    private StockCompanyProfileEntity toProfile(MarketAssetRefVO ref, MarketDetailCommand.Callback c) {
        MarketDetailCommand.StockCompanyProfile p = c.getStockCompanyProfile();
        return StockCompanyProfileEntity.builder().stockCode(ref.getAssetCode()).market(ref.getMarket())
                .companyName(safe(p.getCompanyName(), 200)).industry(safe(p.getIndustry(), 200))
                .businessSummary(safe(p.getBusinessSummary(), 10000)).companyProfile(safe(p.getCompanyProfile(), 10000))
                .website(safe(p.getWebsite(), 500)).sourceAsOf(parseDateOrTime(p.getSourceAsOf()))
                .fetchedAt(DateTimeUtils.now()).build();
    }

    private void saveWarnings(MarketDetailCommand.Callback c) {
        if (c.getRefreshWarnings() == null) return;
        List<ProcessingLogEntity> logs = c.getRefreshWarnings().stream().filter(w -> w != null && w.getEvent() != null)
                .limit(50).map(w -> ProcessingLogEntity.builder().sourceRefId(c.getServerTaskId())
                        .module(safe(w.getModule(), 50) == null ? "market_detail" : safe(w.getModule(), 50))
                        .event(safe(w.getEvent(), 100)).message(safe(w.getMessage(), 500) == null ? "agent warning" : safe(w.getMessage(), 500))
                        .severity(Set.of("info", "warning", "error").contains(w.getSeverity()) ? w.getSeverity() : "warning").build()).toList();
        processingTaskRepository.saveLogs(logs);
    }

    private void recordSlicePersistenceFailure(String serverTaskId, String slice, RuntimeException exception) {
        Throwable root = exception;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String exceptionType = root.getClass().getSimpleName();
        String summary = safe(root.getMessage(), 300);
        if (summary == null) summary = exceptionType;
        log.error("market detail slice persistence failed: serverTaskId={}, slice={}, exceptionType={}, summary={}",
                serverTaskId, slice, exceptionType, summary);
        try {
            processingTaskRepository.saveLogs(List.of(ProcessingLogEntity.builder()
                    .sourceRefId(serverTaskId).module("market_detail_data_refresh")
                    .event(slice + "_persist_failed").message(exceptionType + ": " + summary)
                    .severity("error").build()));
        } catch (RuntimeException diagnosticException) {
            log.warn("market detail persistence diagnostic save failed: serverTaskId={}, slice={}, exceptionType={}",
                    serverTaskId, slice, diagnosticException.getClass().getSimpleName());
        }
    }

    private ProcessingTaskStatusEnumVO callbackTarget(String status, int success, int failed) {
        if ("failed".equals(status) || success == 0 && failed > 0) return ProcessingTaskStatusEnumVO.FAILED;
        if ("partial_failed".equals(status) || failed > 0) return ProcessingTaskStatusEnumVO.PARTIAL_FAILED;
        return ProcessingTaskStatusEnumVO.SUCCEEDED;
    }

    private String taskParams(TaskPlan p) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("assetKind", p.ref.getAssetKind()); params.put("assetRef", p.ref.value());
        params.put("slices", p.slices); params.put("periods", p.periods);
        return JSON.toJSONString(params);
    }

    private List<String> dedup(List<String> values) {
        if (values == null) return List.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) if (value != null && !value.isBlank()) result.add(value.trim());
        return List.copyOf(result);
    }

    private MarketAssetRefVO parseStockRef(String assetRef) {
        try { return MarketAssetRefVO.parse(MarketAssetRefVO.KIND_STOCK, assetRef); }
        catch (IllegalArgumentException e) { throw illegal(e.getMessage()); }
    }

    private StockMarketEntity requireStock(MarketAssetRefVO ref) {
        StockMarketEntity stock = stockMarketRepository.queryOne(ref.getAssetCode(), ref.getMarket());
        if (stock == null) throw illegal("股票不存在");
        return stock;
    }

    private String requireFund(String fundCode) {
        if (fundCode == null || fundCode.isBlank()) throw illegal("基金代码不合法");
        String code = fundCode.trim();
        if (!fundDataRepository.queryExistingFundCodes(List.of(code)).contains(code)) throw illegal("基金不存在");
        return code;
    }

    private List<FundPeriodPerformanceEntity> latestPerformanceSnapshot(String fundCode) {
        List<FundPeriodPerformanceEntity> rows = marketDetailRepository.queryFundPeriodPerformance(fundCode);
        LocalDate latest = rows.stream().map(FundPeriodPerformanceEntity::getAsOf)
                .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null);
        if (latest == null) return List.of();
        return rows.stream().filter(row -> latest.equals(row.getAsOf())).toList();
    }

    private LocalDate parseDate(String value) {
        try { return LocalDate.parse(value); } catch (RuntimeException e) { throw illegal("日期格式不合法"); }
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.trim()); } catch (NumberFormatException e) { throw illegal("十进制字段不合法"); }
    }

    private BigDecimal nonNegative(String value) {
        BigDecimal parsed = decimal(value);
        if (parsed != null && parsed.signum() < 0) throw illegal("价格、净值或成交量不能为负数");
        return parsed;
    }

    private BigDecimal requiredNonNegative(String value) {
        BigDecimal parsed = nonNegative(value);
        if (parsed == null) throw illegal("必填价格或净值不能为空");
        return parsed;
    }

    private LocalDateTime parseTime(String value) {
        return value == null || value.isBlank() ? null : parseTimeRequired(value);
    }

    private LocalDateTime parseDateOrTime(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value).atStartOfDay(); }
        catch (DateTimeParseException ignored) { return parseTimeRequired(value); }
    }

    private LocalDateTime parseTimeRequired(String value) {
        try { return DateTimeUtils.toBusinessLocalDateTime(value); }
        catch (DateTimeParseException | NullPointerException e) { throw illegal("时间格式不合法"); }
    }

    private String format(LocalDateTime value) {
        if (value == null) return null;
        return value.atOffset(BUSINESS_ZONE.getRules().getOffset(value)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String safe(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        String redacted = SENSITIVE_TEXT_PATTERN.matcher(normalized).replaceAll("$1=[redacted]");
        return redacted.length() > max ? redacted.substring(0, max) : redacted;
    }

    private MarketDetailResult.Task toTask(ProcessingTaskEntity task) {
        return MarketDetailResult.Task.builder().serverTaskId(task.getServerTaskId()).taskType(task.getTaskType())
                .status(task.getStatus() == null ? null : task.getStatus().getCode()).build();
    }

    private AppException illegal(String message) {
        return new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), message);
    }

    private record TaskPlan(MarketAssetRefVO ref, List<String> slices, List<String> periods, String providerMarketCode) { }
    private record CallbackPlan(ProcessingTaskEntity task, MarketAssetRefVO ref, List<String> slices, List<String> periods) { }
    private record FundRefreshClaim(ProcessingTaskEntity task, TaskPlan plan, Map<String, String> statuses) { }
}
