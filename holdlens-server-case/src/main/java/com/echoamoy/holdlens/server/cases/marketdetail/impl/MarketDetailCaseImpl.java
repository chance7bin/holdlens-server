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
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.StockDetailSliceStateEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.entity.StockPriceBarEntity;
import com.echoamoy.holdlens.server.domain.marketdetail.model.valobj.MarketDetailDispatchResultVO;
import com.echoamoy.holdlens.server.domain.marketdetail.model.valobj.MarketDetailDataStatusEnumVO;
import com.echoamoy.holdlens.server.domain.marketdetail.model.valobj.MarketDetailFreshnessEnumVO;
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
    private static final int MAX_FUND_NAV_POINTS = 10000;
    private static final Set<String> STOCK_PERIODS = Set.of("intraday", "5d", "1m", "1y");
    private static final List<String> STOCK_DETAIL_PERIODS = List.of("5d", "1m", "1y");
    private static final List<String> STOCK_DETAIL_SLICES = List.of("price_history", "company_profile");
    private static final String STOCK_DETAIL_REQUEST_MODE = "stock_detail_ensure";
    private static final String FUND_DETAIL_REQUEST_MODE = "fund_detail_ensure";
    private static final String STOCK_DETAIL_ACTIVE_KEY_PREFIX = "stock-detail:";
    private static final String FUND_DETAIL_ACTIVE_KEY_PREFIX = "fund-detail:";
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
    @Resource private MarketDetailCallbackValidator callbackValidator;

    @Value("${holdlens.agent.market-detail-data-refresh-callback-url}")
    private String callbackUrl;
    @Value("${holdlens.agent.market-detail-data-refresh.cooldown-minutes:10}")
    private long refreshCooldownMinutes;
    @Value("${holdlens.agent.market-detail-data-refresh.timeout-minutes:10}")
    private long refreshTimeoutMinutes;
    @Value("${holdlens.market-detail.access-write-throttle-minutes:60}")
    private long accessWriteThrottleMinutes;
    @Value("${holdlens.market-detail.active-days:90}")
    private long activeDays;
    @Value("${holdlens.market-detail.schedule-batch-size:100}")
    private int scheduleBatchSize;
    @Value("${holdlens.market-detail.fund-nav-stale-hours:36}")
    private long fundNavStaleHours;
    @Value("${holdlens.market-detail.fund-performance-stale-hours:168}")
    private long fundPerformanceStaleHours;
    @Value("${holdlens.market-detail.stock-price-stale-hours:36}")
    private long stockPriceStaleHours;
    @Value("${holdlens.market-detail.stock-profile-stale-days:30}")
    private long stockProfileStaleDays;

    @Override
    public MarketDetailResult.Task createAndDispatch(MarketDetailCommand.CreateTask command) {
        TaskPlan plan = validateCreate(command);
        ProcessingTaskEntity task = ProcessingTaskEntity.builder()
                .serverTaskId("market_detail_data_refresh_" + UUID.randomUUID().toString().replace("-", ""))
                .taskType(ProcessingTaskEntity.MARKET_DETAIL_DATA_REFRESH)
                .taskParamsJson(taskParams(plan)).status(ProcessingTaskStatusEnumVO.CREATED).build();
        processingTaskRepository.saveTask(task);
        dispatchTask(task, plan);
        processingTaskRepository.updateTaskIfNonTerminal(task);
        return toTask(processingTaskRepository.queryTask(task.getServerTaskId()));
    }

    @Override
    public MarketDetailResult.Task handleCallback(MarketDetailCommand.Callback command) {
        CallbackPlan plan = callbackValidator.validate(command);
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
                String declaredStatus = callbackValidator.sliceResultStatus(command, "nav_history");
                if ("failed".equals(declaredStatus)) {
                    failedSlices++;
                    convergeFundSlice(command, plan.ref.getAssetCode(), "nav_history", "failed", command.getErrorSummary());
                } else if ("empty".equals(declaredStatus)) {
                    successfulSlices++;
                    convergeFundSlice(command, plan.ref.getAssetCode(), "nav_history", "empty", null);
                } else if (command.getFundNavHistory() == null) {
                    if ("available".equals(declaredStatus) || "partial_failed".equals(command.getStatus())) {
                        failedSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "nav_history", "failed", command.getErrorSummary());
                    } else {
                        successfulSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "nav_history", "empty", null);
                    }
                } else {
                    try {
                        List<FundNavHistoryEntity> points = toFundNav(plan.ref, command);
                        if ("available".equals(declaredStatus) && points.isEmpty()) {
                            throw illegal("基金净值 slice 状态与数据不一致");
                        }
                        boolean current = persistFundNavSlice(plan, command, points,
                                points.isEmpty() ? "empty" : "available", null);
                        if (current) successfulSlices++; else failedSlices++;
                    } catch (RuntimeException exception) {
                        failedSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "nav_history", "failed", "nav history rejected");
                        recordSlicePersistenceFailure(command.getServerTaskId(), "nav_history", exception);
                    }
                }
            }
            if (plan.slices.contains("period_performance")) {
                String declaredStatus = callbackValidator.sliceResultStatus(command, "period_performance");
                if ("failed".equals(declaredStatus)) {
                    failedSlices++;
                    convergeFundSlice(command, plan.ref.getAssetCode(), "period_performance", "failed", command.getErrorSummary());
                } else if ("empty".equals(declaredStatus)) {
                    successfulSlices++;
                    convergeFundSlice(command, plan.ref.getAssetCode(), "period_performance", "empty", null);
                } else if (command.getFundPeriodPerformance() == null) {
                    if ("available".equals(declaredStatus) || "partial_failed".equals(command.getStatus())) {
                        failedSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "period_performance", "failed", command.getErrorSummary());
                    } else {
                        successfulSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "period_performance", "empty", null);
                    }
                } else {
                    try {
                        List<FundPeriodPerformanceEntity> rows = toFundPeriodPerformance(plan.ref, command);
                        if ("available".equals(declaredStatus) && rows.isEmpty()) {
                            throw illegal("基金阶段业绩 slice 状态与数据不一致");
                        }
                        boolean current = persistFundPerformanceSlice(plan, command, rows,
                                rows.isEmpty() ? "empty" : "available", null);
                        if (current) successfulSlices++; else failedSlices++;
                    } catch (RuntimeException exception) {
                        failedSlices++;
                        convergeFundSlice(command, plan.ref.getAssetCode(), "period_performance", "failed",
                                "period performance rejected");
                        recordSlicePersistenceFailure(command.getServerTaskId(), "period_performance", exception);
                    }
                }
            }
            if (plan.slices.contains("price_history")) {
                String declaredStatus = callbackValidator.sliceResultStatus(command, "price_history");
                if ("failed".equals(declaredStatus)) {
                    failedSlices++;
                    convergeStockSlice(command, plan.ref.value(), "price_history", "failed", command.getErrorSummary());
                } else try {
                    List<StockPriceBarEntity> bars = command.getStockPriceHistories() == null
                            ? List.of() : toStockBars(plan, command);
                    Set<String> returnedPeriods = command.getStockPriceHistories() == null ? Set.of()
                            : command.getStockPriceHistories().stream().filter(java.util.Objects::nonNull)
                            .map(MarketDetailCommand.StockPriceHistory::getPeriod).collect(java.util.stream.Collectors.toSet());
                    boolean complete = returnedPeriods.size() == plan.periods.size()
                            && returnedPeriods.containsAll(plan.periods);
                    String sliceStatus = "empty".equals(declaredStatus) ? "empty"
                            : complete ? (bars.isEmpty() ? "empty" : "available") : "failed";
                    if ("available".equals(declaredStatus) && (!complete || bars.isEmpty())
                            || "empty".equals(declaredStatus) && !bars.isEmpty()) {
                        throw illegal("股票价格历史 slice 状态与数据不一致");
                    }
                    boolean current = persistStockPriceSlice(plan, command, bars, sliceStatus,
                            complete || "empty".equals(declaredStatus) ? null : command.getErrorSummary());
                    if ((complete || "empty".equals(declaredStatus)) && current) successfulSlices++; else failedSlices++;
                } catch (RuntimeException exception) {
                    failedSlices++;
                    convergeStockSlice(command, plan.ref.value(), "price_history", "failed", "price history rejected");
                    recordSlicePersistenceFailure(command.getServerTaskId(), "price_history", exception);
                }
            }
            if (plan.slices.contains("company_profile")) {
                String declaredStatus = callbackValidator.sliceResultStatus(command, "company_profile");
                if ("failed".equals(declaredStatus)) {
                    failedSlices++;
                    convergeStockSlice(command, plan.ref.value(), "company_profile", "failed", command.getErrorSummary());
                } else try {
                    StockCompanyProfileEntity profile = command.getStockCompanyProfile() == null
                            ? null : toProfile(plan.ref, command);
                    boolean complete = command.getStockCompanyProfile() != null || "empty".equals(declaredStatus);
                    String sliceStatus = !complete ? "failed" : hasProfileData(profile) ? "available" : "empty";
                    if ("available".equals(declaredStatus) && !hasProfileData(profile)
                            || "empty".equals(declaredStatus) && hasProfileData(profile)) {
                        throw illegal("股票公司资料 slice 状态与数据不一致");
                    }
                    boolean current = persistStockCompanyProfileSlice(plan, command, profile, sliceStatus,
                            complete ? null : command.getErrorSummary());
                    if (complete && current) successfulSlices++; else failedSlices++;
                } catch (RuntimeException exception) {
                    failedSlices++;
                    convergeStockSlice(command, plan.ref.value(), "company_profile", "failed", "company profile rejected");
                    recordSlicePersistenceFailure(command.getServerTaskId(), "company_profile", exception);
                }
            }
        }
        if ("failed".equals(command.getStatus()) && MarketAssetRefVO.KIND_FUND.equals(plan.ref.getAssetKind())) {
            for (String slice : plan.slices) {
                convergeFundSlice(command, plan.ref.getAssetCode(), slice, "failed", command.getErrorSummary());
                failedSlices++;
            }
        }
        if ("failed".equals(command.getStatus()) && MarketAssetRefVO.KIND_STOCK.equals(plan.ref.getAssetKind())) {
            for (String slice : plan.slices) {
                convergeStockSlice(command, plan.ref.value(), slice, "failed", command.getErrorSummary());
                failedSlices++;
            }
        }

        ProcessingTaskStatusEnumVO target = callbackTarget(command.getStatus(), successfulSlices, failedSlices);
        ProcessingTaskEntity latest = processingTaskRepository.queryTask(command.getServerTaskId());
        if (!latest.isTerminal()) {
            latest.transitTo(target, safe(command.getErrorSummary(), 500));
            processingTaskRepository.updateTaskIfNonTerminal(latest);
            latest = processingTaskRepository.queryTask(command.getServerTaskId());
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
        return toLegacyFundRefresh(ensureFundDetailData(fundCode, true));
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

    @Override
    public MarketDetailResult.StockDetailRefresh ensureStockDetailData(String assetRef) {
        return toLegacyStockRefresh(ensureStockDetailDataV2(assetRef, true));
    }

    @Override
    public MarketDetailResult.StockDetailRefresh queryStockDetailDataTask(String serverTaskId) {
        MarketDetailResult.DetailRefresh refresh = queryDetailOperation(serverTaskId);
        if (!MarketAssetRefVO.KIND_STOCK.equals(refresh.getAssetKind())) throw illegal("未知股票详情任务");
        return toLegacyStockRefresh(refresh);
    }

    @Override
    public MarketDetailResult.DetailRefresh ensureFundDetailData(String fundCode, boolean recordView) {
        String code = requireFund(fundCode);
        LocalDateTime now = DateTimeUtils.now();
        if (recordView) {
            fundDataRepository.markDetailViewed(List.of(code), now,
                    now.minusMinutes(Math.max(accessWriteThrottleMinutes, 1)));
        }
        FundRefreshClaim claim = transactionExecutor.required(() -> claimFundDetailRefresh(code));
        dispatchFundClaim(code, claim);
        MarketAssetRefVO ref = MarketAssetRefVO.parse(MarketAssetRefVO.KIND_FUND, "fund:" + code);
        return toDetailRefresh(ref, claim.serverTaskId);
    }

    @Override
    public MarketDetailResult.DetailRefresh ensureStockDetailDataV2(String assetRef, boolean recordView) {
        MarketAssetRefVO ref = parseStockRef(assetRef);
        StockMarketEntity stock = requireStock(ref);
        LocalDateTime now = DateTimeUtils.now();
        if (recordView) {
            stockMarketRepository.markDetailViewed(ref.getAssetCode(), ref.getMarket(), now,
                    now.minusMinutes(Math.max(accessWriteThrottleMinutes, 1)));
        }
        StockRefreshClaim claim = transactionExecutor.required(() -> claimStockDetailRefresh(ref, stock));
        dispatchStockClaim(ref, claim);
        return toDetailRefresh(ref, claim.serverTaskId);
    }

    @Override
    public MarketDetailResult.DetailRefresh queryDetailOperation(String operationId) {
        if (operationId == null || operationId.isBlank()) throw illegal("操作标识不能为空");
        ProcessingTaskEntity task = processingTaskRepository.queryTask(operationId.trim());
        if (task == null || !ProcessingTaskEntity.MARKET_DETAIL_DATA_REFRESH.equals(task.getTaskType())
                || task.getTaskParamsJson() == null) {
            throw illegal("未知详情数据操作");
        }
        JSONObject params = JSON.parseObject(task.getTaskParamsJson());
        String requestMode = params.getString("requestMode");
        if (!Set.of(FUND_DETAIL_REQUEST_MODE, STOCK_DETAIL_REQUEST_MODE).contains(requestMode)) {
            throw illegal("未知详情数据操作");
        }
        MarketAssetRefVO ref;
        try {
            ref = MarketAssetRefVO.parse(params.getString("assetKind"), params.getString("assetRef"));
        } catch (IllegalArgumentException exception) {
            throw illegal("详情数据操作引用不合法");
        }
        if (MarketAssetRefVO.KIND_FUND.equals(ref.getAssetKind())) requireFund(ref.getAssetCode());
        else requireStock(ref);
        return toDetailRefresh(ref, task.getServerTaskId());
    }

    @Override
    public int scheduleActiveFundDetails() {
        LocalDateTime viewedSince = DateTimeUtils.now().minusDays(Math.max(activeDays, 1));
        int dispatched = 0;
        for (String fundCode : fundDataRepository.queryDetailRefreshTargets(
                viewedSince, Math.max(scheduleBatchSize, 1))) {
            try {
                if (ensureFundDetailData(fundCode, false).getOperationId() != null) dispatched++;
            } catch (RuntimeException exception) {
                log.warn("活动基金详情刷新领取失败 fundCode={}", fundCode);
            }
        }
        return dispatched;
    }

    @Override
    public int scheduleActiveStockDetails(String market) {
        LocalDateTime viewedSince = DateTimeUtils.now().minusDays(Math.max(activeDays, 1));
        int dispatched = 0;
        for (StockMarketEntity stock : stockMarketRepository.queryDetailRefreshTargets(
                market, viewedSince, Math.max(scheduleBatchSize, 1))) {
            String assetRef = "stock:" + stock.getMarket() + ":" + stock.getStockCode();
            try {
                if (ensureStockDetailDataV2(assetRef, false).getOperationId() != null) dispatched++;
            } catch (RuntimeException exception) {
                log.warn("活动股票详情刷新领取失败 market={} stockCode={}", stock.getMarket(), stock.getStockCode());
            }
        }
        return dispatched;
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
        String exchangeCode = null;
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
            if (MarketAssetRefVO.MARKET_A_SHARE.equals(ref.getMarket()) && slices.contains("price_history")) {
                exchangeCode = requireAShareExchangeCode(stock, ref.getAssetCode());
            }
        }
        return new TaskPlan(ref, slices, periods, providerCode, exchangeCode);
    }

    private FundRefreshClaim claimFundDetailRefresh(String fundCode) {
        LocalDateTime now = DateTimeUtils.now();
        String activeKey = FUND_DETAIL_ACTIVE_KEY_PREFIX + fundCode;
        marketDetailRepository.ensureFundSliceStates(fundCode, FUND_DETAIL_SLICES);
        Map<String, MarketDetailSliceStateEntity> states = new LinkedHashMap<>();
        for (String slice : FUND_DETAIL_SLICES) {
            states.put(slice, marketDetailRepository.lockFundSliceState(fundCode, slice));
        }
        Map<String, String> statuses = new LinkedHashMap<>();
        List<String> claimable = new ArrayList<>();
        Map<String, SliceFact> facts = fundSliceFacts(fundCode, now);
        String activeTaskId = null;
        for (String slice : FUND_DETAIL_SLICES) {
            MarketDetailSliceStateEntity state = states.get(slice);
            SliceFact fact = facts.get(slice);
            if (fact.hasData) statuses.put(slice, "available");
            if (fact.hasData && !fact.stale) {
                state.setStatus("available");
                state.setActiveTaskId(null);
                state.setLastSuccessAt(fact.fetchedAt);
                state.setErrorSummary(null);
                marketDetailRepository.updateFundSliceState(state);
                continue;
            }
            if ("refreshing".equals(state.getStatus()) && state.getActiveTaskId() != null) {
                ProcessingTaskEntity active = processingTaskRepository.queryTaskForUpdate(state.getActiveTaskId());
                boolean timedOut = state.getLastAttemptAt() == null
                        || state.getLastAttemptAt().isBefore(now.minusMinutes(refreshTimeoutMinutes));
                boolean leaseValid = active != null && !active.isTerminal() && !timedOut
                        && activeKey.equals(active.getActiveKey())
                        && (active.getLeaseUntil() == null || active.getLeaseUntil().isAfter(now));
                if (leaseValid) {
                    activeTaskId = active.getServerTaskId();
                    if (!fact.hasData) statuses.put(slice, "refreshing");
                    continue;
                }
                expireDetailTask(active, activeKey, now);
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
                if (!fact.hasData) statuses.put(slice, state.getStatus());
            } else {
                claimable.add(slice);
            }
        }
        if (activeTaskId != null) return new FundRefreshClaim(null, null, statuses, activeTaskId);
        if (claimable.isEmpty()) return new FundRefreshClaim(null, null, statuses, null);
        MarketAssetRefVO ref = MarketAssetRefVO.parse(MarketAssetRefVO.KIND_FUND, "fund:" + fundCode);
        TaskPlan plan = new TaskPlan(ref, List.copyOf(claimable), List.of(), null, null);
        ProcessingTaskEntity task = ProcessingTaskEntity.builder()
                .serverTaskId("market_detail_data_refresh_" + UUID.randomUUID().toString().replace("-", ""))
                .taskType(ProcessingTaskEntity.MARKET_DETAIL_DATA_REFRESH)
                .taskParamsJson(taskParams(plan, FUND_DETAIL_REQUEST_MODE))
                .activeKey(activeKey).leaseUntil(now.plusMinutes(refreshTimeoutMinutes))
                .status(ProcessingTaskStatusEnumVO.CREATED).build();
        if (!processingTaskRepository.saveTaskIfActiveKeyAbsent(task)) {
            ProcessingTaskEntity existing = processingTaskRepository.queryTaskByActiveKey(activeKey);
            if (existing == null || existing.isTerminal()) throw illegal("基金详情刷新任务竞争失败，请稍后重试");
            List<String> slices = slicesFromTask(existing, FUND_DETAIL_SLICES);
            markFundSlicesRefreshing(states, slices, existing.getServerTaskId(), now);
            for (String slice : slices) {
                if (!facts.get(slice).hasData) statuses.put(slice, "refreshing");
            }
            return new FundRefreshClaim(null, null, statuses, existing.getServerTaskId());
        }
        markFundSlicesRefreshing(states, claimable, task.getServerTaskId(), now);
        for (String slice : claimable) {
            if (!facts.get(slice).hasData) statuses.put(slice, "refreshing");
        }
        return new FundRefreshClaim(task, plan, statuses, task.getServerTaskId());
    }

    private StockRefreshClaim claimStockDetailRefresh(MarketAssetRefVO ref, StockMarketEntity stock) {
        String assetRef = ref.value();
        String activeKey = STOCK_DETAIL_ACTIVE_KEY_PREFIX + assetRef;
        LocalDateTime now = DateTimeUtils.now();
        marketDetailRepository.ensureStockSliceStates(assetRef, STOCK_DETAIL_SLICES);

        Map<String, StockDetailSliceStateEntity> states = new LinkedHashMap<>();
        for (String slice : STOCK_DETAIL_SLICES) {
            states.put(slice, marketDetailRepository.lockStockSliceState(assetRef, slice));
        }

        Map<String, SliceFact> facts = stockSliceFacts(ref, now);
        String activeTaskId = null;
        List<String> claimable = new ArrayList<>();

        for (String slice : STOCK_DETAIL_SLICES) {
            StockDetailSliceStateEntity state = states.get(slice);
            SliceFact fact = facts.get(slice);
            if (fact.hasData && !fact.stale) {
                state.setStatus("available");
                state.setActiveTaskId(null);
                state.setLastSuccessAt(fact.fetchedAt);
                state.setErrorSummary(null);
                marketDetailRepository.updateStockSliceState(state);
                continue;
            }

            boolean expiredActiveTask = false;
            if ("refreshing".equals(state.getStatus()) && state.getActiveTaskId() != null) {
                ProcessingTaskEntity active = processingTaskRepository.queryTaskForUpdate(state.getActiveTaskId());
                boolean leaseValid = active != null && !active.isTerminal()
                        && activeKey.equals(active.getActiveKey())
                        && active.getLeaseUntil() != null && active.getLeaseUntil().isAfter(now);
                if (leaseValid) {
                    activeTaskId = active.getServerTaskId();
                    continue;
                }
                expireDetailTask(active, activeKey, now);
                state.setStatus("failed");
                state.setActiveTaskId(null);
                state.setLastAttemptAt(now);
                state.setErrorSummary("market detail refresh timed out");
                marketDetailRepository.updateStockSliceState(state);
                expiredActiveTask = true;
            }

            if (expiredActiveTask) {
                claimable.add(slice);
                continue;
            }
            boolean cooling = state.getLastAttemptAt() != null
                    && state.getLastAttemptAt().isAfter(now.minusMinutes(refreshCooldownMinutes))
                    && Set.of("empty", "failed").contains(state.getStatus());
            if (!cooling) claimable.add(slice);
        }

        if (activeTaskId != null) return new StockRefreshClaim(null, null, activeTaskId);
        if (claimable.isEmpty()) return new StockRefreshClaim(null, null, null);

        List<String> periods = claimable.contains("price_history") ? STOCK_DETAIL_PERIODS : List.of();
        String providerCode = null;
        String exchangeCode = null;
        if (claimable.contains("price_history")) {
            if (MarketAssetRefVO.MARKET_US_STOCK.equals(ref.getMarket())) {
                providerCode = stock.getProviderMarketCode();
                if (providerCode == null || providerCode.isBlank()) throw illegal("美股缺少 provider_market_code");
            } else if (MarketAssetRefVO.MARKET_A_SHARE.equals(ref.getMarket())) {
                exchangeCode = requireAShareExchangeCode(stock, ref.getAssetCode());
            }
        }
        TaskPlan plan = new TaskPlan(ref, List.copyOf(claimable), periods, providerCode, exchangeCode);
        ProcessingTaskEntity task = ProcessingTaskEntity.builder()
                .serverTaskId("market_detail_data_refresh_" + UUID.randomUUID().toString().replace("-", ""))
                .taskType(ProcessingTaskEntity.MARKET_DETAIL_DATA_REFRESH)
                .taskParamsJson(taskParams(plan, STOCK_DETAIL_REQUEST_MODE))
                .activeKey(activeKey).leaseUntil(now.plusMinutes(refreshTimeoutMinutes))
                .status(ProcessingTaskStatusEnumVO.CREATED).build();
        if (!processingTaskRepository.saveTaskIfActiveKeyAbsent(task)) {
            ProcessingTaskEntity existing = processingTaskRepository.queryTaskByActiveKey(activeKey);
            if (existing == null || existing.isTerminal()) throw illegal("股票详情刷新任务竞争失败，请稍后重试");
            markStockSlicesRefreshing(states, slicesFromTask(existing, STOCK_DETAIL_SLICES), existing.getServerTaskId(), now);
            return new StockRefreshClaim(null, null, existing.getServerTaskId());
        }

        markStockSlicesRefreshing(states, claimable, task.getServerTaskId(), now);
        return new StockRefreshClaim(task, plan, task.getServerTaskId());
    }

    private void expireDetailTask(ProcessingTaskEntity task, String activeKey, LocalDateTime now) {
        if (task == null || task.isTerminal()) return;
        if (task.getLeaseUntil() != null) {
            processingTaskRepository.markFailedIfLeaseExpired(task.getServerTaskId(), activeKey, now,
                    "market detail refresh timed out");
            return;
        }
        task.transitTo(ProcessingTaskStatusEnumVO.FAILED, "market detail refresh timed out");
        processingTaskRepository.updateTaskIfNonTerminal(task);
    }

    private void markStockSlicesRefreshing(Map<String, StockDetailSliceStateEntity> states, List<String> slices,
                                           String serverTaskId, LocalDateTime now) {
        for (String slice : slices) {
            StockDetailSliceStateEntity state = states.get(slice);
            if (state == null) continue;
            state.setStatus("refreshing");
            state.setActiveTaskId(serverTaskId);
            state.setLastAttemptAt(now);
            state.setErrorSummary(null);
            marketDetailRepository.updateStockSliceState(state);
        }
    }

    private void markFundSlicesRefreshing(Map<String, MarketDetailSliceStateEntity> states, List<String> slices,
                                          String serverTaskId, LocalDateTime now) {
        for (String slice : slices) {
            MarketDetailSliceStateEntity state = states.get(slice);
            if (state == null) continue;
            state.setStatus("refreshing");
            state.setActiveTaskId(serverTaskId);
            state.setLastAttemptAt(now);
            state.setErrorSummary(null);
            marketDetailRepository.updateFundSliceState(state);
        }
    }

    private List<String> slicesFromTask(ProcessingTaskEntity task, List<String> supportedSlices) {
        if (task == null || task.getTaskParamsJson() == null) return List.of();
        JSONObject params = JSON.parseObject(task.getTaskParamsJson());
        if (params.getJSONArray("slices") == null) return List.of();
        return params.getJSONArray("slices").toJavaList(String.class).stream()
                .filter(supportedSlices::contains).toList();
    }

    private Map<String, SliceFact> fundSliceFacts(String fundCode, LocalDateTime now) {
        LocalDateTime navFetchedAt = marketDetailRepository.queryLatestFundNavFetchedAt(fundCode);
        List<FundPeriodPerformanceEntity> performance = latestPerformanceSnapshot(fundCode);
        LocalDateTime performanceFetchedAt = performance.stream().map(FundPeriodPerformanceEntity::getFetchedAt)
                .filter(java.util.Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
        Map<String, SliceFact> facts = new LinkedHashMap<>();
        facts.put("nav_history", SliceFact.of(marketDetailRepository.queryLatestFundNavDate(fundCode) != null,
                navFetchedAt, now.minusHours(Math.max(fundNavStaleHours, 1))));
        facts.put("period_performance", SliceFact.of(!performance.isEmpty(), performanceFetchedAt,
                now.minusHours(Math.max(fundPerformanceStaleHours, 1))));
        return facts;
    }

    private Map<String, SliceFact> stockSliceFacts(MarketAssetRefVO ref, LocalDateTime now) {
        LocalDateTime priceFetchedAt = marketDetailRepository.queryLatestStockBarFetchedAt(
                ref.getAssetCode(), ref.getMarket(), "day");
        StockCompanyProfileEntity profile = marketDetailRepository.queryStockCompanyProfile(
                ref.getAssetCode(), ref.getMarket());
        Map<String, SliceFact> facts = new LinkedHashMap<>();
        facts.put("price_history", SliceFact.of(marketDetailRepository.queryLatestStockBarTime(
                        ref.getAssetCode(), ref.getMarket(), "day") != null, priceFetchedAt,
                now.minusHours(Math.max(stockPriceStaleHours, 1))));
        facts.put("company_profile", SliceFact.of(hasProfileData(profile),
                profile == null ? null : profile.getFetchedAt(),
                now.minusDays(Math.max(stockProfileStaleDays, 1))));
        return facts;
    }

    private MarketDetailResult.DetailRefresh toDetailRefresh(MarketAssetRefVO ref, String operationId) {
        LocalDateTime now = DateTimeUtils.now();
        Map<String, SliceFact> facts = MarketAssetRefVO.KIND_FUND.equals(ref.getAssetKind())
                ? fundSliceFacts(ref.getAssetCode(), now) : stockSliceFacts(ref, now);
        Map<String, SliceStateView> states = new LinkedHashMap<>();
        if (MarketAssetRefVO.KIND_FUND.equals(ref.getAssetKind())) {
            marketDetailRepository.queryFundSliceStates(ref.getAssetCode()).forEach(state -> states.put(
                    state.getSliceType(), new SliceStateView(state.getStatus(), state.getActiveTaskId(), state.getLastAttemptAt())));
        } else {
            marketDetailRepository.queryStockSliceStates(ref.value()).forEach(state -> states.put(
                    state.getSliceType(), new SliceStateView(state.getStatus(), state.getActiveTaskId(), state.getLastAttemptAt())));
        }
        List<String> sliceNames = MarketAssetRefVO.KIND_FUND.equals(ref.getAssetKind())
                ? FUND_DETAIL_SLICES : STOCK_DETAIL_SLICES;
        List<MarketDetailResult.DetailSlice> slices = sliceNames.stream()
                .map(slice -> toDetailSlice(slice, facts.get(slice), states.get(slice), now)).toList();
        String overall = detailOverallStatus(slices);
        return MarketDetailResult.DetailRefresh.builder().assetKind(ref.getAssetKind()).assetRef(ref.value())
                .operationId(operationId).status(overall).retryAfterMs(1000L).slices(slices).build();
    }

    private MarketDetailResult.DetailSlice toDetailSlice(String slice, SliceFact fact, SliceStateView state,
                                                         LocalDateTime now) {
        if (fact != null && fact.hasData) {
            return MarketDetailResult.DetailSlice.builder().slice(slice)
                    .status(MarketDetailDataStatusEnumVO.AVAILABLE.name())
                    .freshness(fact.stale ? MarketDetailFreshnessEnumVO.STALE.name()
                            : MarketDetailFreshnessEnumVO.FRESH.name())
                    .hasData(true).build();
        }
        String persisted = state == null || state.status == null ? "missing" : state.status;
        String status;
        if ("refreshing".equals(persisted) && isActiveOperation(state, now)) {
            status = MarketDetailDataStatusEnumVO.PROCESSING.name();
        } else if ("empty".equals(persisted)) {
            status = MarketDetailDataStatusEnumVO.EMPTY.name();
        } else if ("failed".equals(persisted) || "refreshing".equals(persisted)) {
            status = MarketDetailDataStatusEnumVO.FAILED.name();
        } else {
            status = MarketDetailDataStatusEnumVO.MISSING.name();
        }
        return MarketDetailResult.DetailSlice.builder().slice(slice).status(status).hasData(false).build();
    }

    private boolean isActiveOperation(SliceStateView state, LocalDateTime now) {
        if (state == null || state.activeTaskId == null) return false;
        ProcessingTaskEntity task = processingTaskRepository.queryTask(state.activeTaskId);
        if (task == null || task.isTerminal()) return false;
        if (task.getLeaseUntil() != null) return task.getLeaseUntil().isAfter(now);
        return state.lastAttemptAt != null
                && state.lastAttemptAt.isAfter(now.minusMinutes(Math.max(refreshTimeoutMinutes, 1)));
    }

    private String detailOverallStatus(List<MarketDetailResult.DetailSlice> slices) {
        if (slices.stream().anyMatch(slice -> !Boolean.TRUE.equals(slice.getHasData())
                && MarketDetailDataStatusEnumVO.PROCESSING.name().equals(slice.getStatus()))) {
            return MarketDetailDataStatusEnumVO.PROCESSING.name();
        }
        if (slices.stream().anyMatch(slice -> Boolean.TRUE.equals(slice.getHasData()))) {
            return MarketDetailDataStatusEnumVO.AVAILABLE.name();
        }
        if (slices.stream().allMatch(slice -> MarketDetailDataStatusEnumVO.EMPTY.name().equals(slice.getStatus()))) {
            return MarketDetailDataStatusEnumVO.EMPTY.name();
        }
        if (slices.stream().anyMatch(slice -> MarketDetailDataStatusEnumVO.FAILED.name().equals(slice.getStatus()))) {
            return MarketDetailDataStatusEnumVO.FAILED.name();
        }
        return MarketDetailDataStatusEnumVO.MISSING.name();
    }

    private void dispatchFundClaim(String fundCode, FundRefreshClaim claim) {
        if (claim.task == null) return;
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
        if (persisted == null || persisted.getStatus() != ProcessingTaskStatusEnumVO.DISPATCH_FAILED) return;
        transactionExecutor.required(() -> {
            for (String slice : claim.plan.slices) {
                marketDetailRepository.updateFundSliceStateIfActiveTask(MarketDetailSliceStateEntity.builder()
                        .fundCode(fundCode).sliceType(slice).status("failed")
                        .activeTaskId(claim.task.getServerTaskId()).lastAttemptAt(DateTimeUtils.now())
                        .errorSummary("agent dispatch failed").build());
            }
            return null;
        });
    }

    private void dispatchStockClaim(MarketAssetRefVO ref, StockRefreshClaim claim) {
        if (claim.taskToDispatch == null) return;
        dispatchTask(claim.taskToDispatch, claim.plan);
        processingTaskRepository.updateTaskIfNonTerminal(claim.taskToDispatch);
        ProcessingTaskEntity persisted = processingTaskRepository.queryTask(claim.taskToDispatch.getServerTaskId());
        if (persisted == null || persisted.getStatus() != ProcessingTaskStatusEnumVO.DISPATCH_FAILED) return;
        transactionExecutor.required(() -> {
            for (String slice : claim.plan.slices) {
                marketDetailRepository.updateStockSliceStateIfActiveTask(StockDetailSliceStateEntity.builder()
                        .assetRef(ref.value()).sliceType(slice).status("failed")
                        .activeTaskId(claim.taskToDispatch.getServerTaskId()).lastAttemptAt(DateTimeUtils.now())
                        .errorSummary("agent dispatch failed").build());
            }
            return null;
        });
    }

    private MarketDetailResult.FundDetailRefresh toLegacyFundRefresh(MarketDetailResult.DetailRefresh refresh) {
        List<MarketDetailResult.FundDetailSlice> slices = refresh.getSlices().stream()
                .map(slice -> MarketDetailResult.FundDetailSlice.builder().slice(slice.getSlice())
                        .status(toLegacySliceStatus(slice.getStatus())).build()).toList();
        String status = MarketDetailDataStatusEnumVO.AVAILABLE.name().equals(refresh.getStatus()) ? "ready"
                : MarketDetailDataStatusEnumVO.PROCESSING.name().equals(refresh.getStatus()) ? "refreshing" : "unavailable";
        return MarketDetailResult.FundDetailRefresh.builder().fundCode(refresh.getAssetRef().substring("fund:".length()))
                .status(status).retryAfterMs(refresh.getRetryAfterMs()).slices(slices).build();
    }

    private MarketDetailResult.StockDetailRefresh toLegacyStockRefresh(MarketDetailResult.DetailRefresh refresh) {
        List<MarketDetailResult.StockDetailSlice> slices = refresh.getSlices().stream()
                .map(slice -> MarketDetailResult.StockDetailSlice.builder().slice(slice.getSlice())
                        .status(toLegacySliceStatus(slice.getStatus())).build()).toList();
        List<String> statuses = slices.stream().map(MarketDetailResult.StockDetailSlice::getStatus).toList();
        return MarketDetailResult.StockDetailRefresh.builder().assetRef(refresh.getAssetRef())
                .serverTaskId(refresh.getOperationId()).status(stockDetailOverallStatus(statuses))
                .retryAfterMs(refresh.getRetryAfterMs()).slices(slices).build();
    }

    private String toLegacySliceStatus(String status) {
        if (MarketDetailDataStatusEnumVO.AVAILABLE.name().equals(status)) return "available";
        if (MarketDetailDataStatusEnumVO.PROCESSING.name().equals(status)) return "refreshing";
        if (MarketDetailDataStatusEnumVO.EMPTY.name().equals(status)) return "empty";
        return "failed";
    }

    private MarketDetailResult.StockDetailRefresh toStockDetailRefresh(String assetRef, String serverTaskId) {
        Map<String, StockDetailSliceStateEntity> states = marketDetailRepository.queryStockSliceStates(assetRef).stream()
                .collect(java.util.stream.Collectors.toMap(StockDetailSliceStateEntity::getSliceType,
                        state -> state, (left, right) -> left, LinkedHashMap::new));
        ProcessingTaskEntity task = serverTaskId == null ? null : processingTaskRepository.queryTask(serverTaskId);
        LocalDateTime now = DateTimeUtils.now();
        List<MarketDetailResult.StockDetailSlice> slices = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        for (String slice : STOCK_DETAIL_SLICES) {
            StockDetailSliceStateEntity state = states.get(slice);
            String status = state == null || state.getStatus() == null ? "failed" : state.getStatus();
            if ("refreshing".equals(status) && task != null
                    && task.getServerTaskId().equals(state.getActiveTaskId())
                    && (task.isTerminal() || task.getLeaseUntil() == null || !task.getLeaseUntil().isAfter(now))) {
                status = "failed";
            }
            statuses.add(status);
            slices.add(MarketDetailResult.StockDetailSlice.builder().slice(slice).status(status).build());
        }
        String overall = stockDetailOverallStatus(statuses);
        return MarketDetailResult.StockDetailRefresh.builder().assetRef(assetRef).serverTaskId(serverTaskId)
                .status(overall).retryAfterMs(1000L).slices(List.copyOf(slices)).build();
    }

    private String stockDetailOverallStatus(List<String> statuses) {
        if (statuses.stream().anyMatch("refreshing"::equals)) return "refreshing";
        if (statuses.stream().allMatch("available"::equals)) return "ready";
        long failures = statuses.stream().filter("failed"::equals).count();
        if (failures == statuses.size()) return "failed";
        if (failures > 0) return "partial_failed";
        return "empty";
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
                            .assetRef(plan.ref.value()).exchangeCode(plan.exchangeCode)
                            .providerMarketCode(plan.providerMarketCode)
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

    private boolean persistFundNavSlice(CallbackPlan plan, MarketDetailCommand.Callback command,
                                        List<FundNavHistoryEntity> points, String status, String errorSummary) {
        if (!isFundDetailEnsureTask(plan.task)) {
            transactionExecutor.requiresNew(() -> { marketDetailRepository.upsertFundNavHistory(points); return null; });
            return true;
        }
        return transactionExecutor.requiresNew(() -> {
            boolean current = updateFundSliceState(command, plan.ref.getAssetCode(), "nav_history",
                    status, errorSummary);
            if (current) marketDetailRepository.upsertFundNavHistory(points);
            return current;
        });
    }

    private boolean persistFundPerformanceSlice(CallbackPlan plan, MarketDetailCommand.Callback command,
                                                List<FundPeriodPerformanceEntity> rows, String status,
                                                String errorSummary) {
        if (!isFundDetailEnsureTask(plan.task)) {
            transactionExecutor.requiresNew(() -> {
                marketDetailRepository.upsertFundPeriodPerformance(rows);
                return null;
            });
            return true;
        }
        return transactionExecutor.requiresNew(() -> {
            boolean current = updateFundSliceState(command, plan.ref.getAssetCode(), "period_performance",
                    status, errorSummary);
            if (current) marketDetailRepository.upsertFundPeriodPerformance(rows);
            return current;
        });
    }

    private boolean updateFundSliceState(MarketDetailCommand.Callback command, String fundCode, String slice,
                                         String status, String errorSummary) {
        LocalDateTime now = DateTimeUtils.now();
        return marketDetailRepository.updateFundSliceStateIfActiveTask(MarketDetailSliceStateEntity.builder()
                .fundCode(fundCode).sliceType(slice).status(status).activeTaskId(command.getServerTaskId())
                .lastAttemptAt(now).lastSuccessAt(Set.of("available", "empty").contains(status) ? now : null)
                .errorSummary(safe(errorSummary, 500)).build());
    }

    private boolean persistStockPriceSlice(CallbackPlan plan, MarketDetailCommand.Callback command,
                                           List<StockPriceBarEntity> bars, String status, String errorSummary) {
        if (!isStockDetailEnsureTask(plan.task)) {
            transactionExecutor.requiresNew(() -> { marketDetailRepository.upsertStockPriceBars(bars); return null; });
            return true;
        }
        return transactionExecutor.requiresNew(() -> {
            boolean current = updateStockSliceState(command, plan.ref.value(), "price_history", status, errorSummary);
            if (current) marketDetailRepository.upsertStockPriceBars(bars);
            return current;
        });
    }

    private boolean persistStockCompanyProfileSlice(CallbackPlan plan, MarketDetailCommand.Callback command,
                                                     StockCompanyProfileEntity profile, String status,
                                                     String errorSummary) {
        if (!isStockDetailEnsureTask(plan.task)) {
            if (profile != null && "available".equals(status)) {
                transactionExecutor.requiresNew(() -> { marketDetailRepository.upsertStockCompanyProfile(profile); return null; });
            }
            return true;
        }
        return transactionExecutor.requiresNew(() -> {
            boolean current = updateStockSliceState(command, plan.ref.value(), "company_profile", status, errorSummary);
            if (current && profile != null && "available".equals(status)) {
                marketDetailRepository.upsertStockCompanyProfile(profile);
            }
            return current;
        });
    }

    private void convergeStockSlice(MarketDetailCommand.Callback command, String assetRef, String slice,
                                    String status, String errorSummary) {
        try {
            transactionExecutor.requiresNew(() -> {
                updateStockSliceState(command, assetRef, slice, status, errorSummary);
                return null;
            });
        } catch (RuntimeException exception) {
            recordSlicePersistenceFailure(command.getServerTaskId(), slice + "_state", exception);
        }
    }

    private boolean updateStockSliceState(MarketDetailCommand.Callback command, String assetRef, String slice,
                                          String status, String errorSummary) {
        LocalDateTime now = DateTimeUtils.now();
        return marketDetailRepository.updateStockSliceStateIfActiveTask(StockDetailSliceStateEntity.builder()
                .assetRef(assetRef).sliceType(slice).status(status).activeTaskId(command.getServerTaskId())
                .lastAttemptAt(now).lastSuccessAt(Set.of("available", "empty").contains(status) ? now : null)
                .errorSummary(safe(errorSummary, 500)).build());
    }

    private boolean isStockDetailEnsureTask(ProcessingTaskEntity task) {
        if (task == null || task.getTaskParamsJson() == null) return false;
        return STOCK_DETAIL_REQUEST_MODE.equals(JSON.parseObject(task.getTaskParamsJson()).getString("requestMode"));
    }

    private boolean isFundDetailEnsureTask(ProcessingTaskEntity task) {
        if (task == null || task.getTaskParamsJson() == null) return false;
        return FUND_DETAIL_REQUEST_MODE.equals(JSON.parseObject(task.getTaskParamsJson()).getString("requestMode"));
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
                    || (Set.of("5d", "1m", "1y").contains(history.getPeriod()) && !"day".equals(history.getGranularity()))) {
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
        LocalDateTime sourceAsOf = parseDateOrTime(p.getSourceAsOf());
        if (sourceAsOf == null) sourceAsOf = parseTime(c.getGeneratedAt());
        return StockCompanyProfileEntity.builder().stockCode(ref.getAssetCode()).market(ref.getMarket())
                .companyName(safe(p.getCompanyName(), 200)).industry(safe(p.getIndustry(), 200))
                .businessSummary(safe(p.getBusinessSummary(), 10000)).companyProfile(safe(p.getCompanyProfile(), 10000))
                .website(safe(p.getWebsite(), 500)).sourceAsOf(sourceAsOf)
                .fetchedAt(DateTimeUtils.now()).build();
    }

    private boolean hasProfileData(StockCompanyProfileEntity profile) {
        return profile != null && (profile.getCompanyName() != null || profile.getIndustry() != null
                || profile.getBusinessSummary() != null || profile.getCompanyProfile() != null
                || profile.getWebsite() != null);
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
        return taskParams(p, null);
    }

    private String taskParams(TaskPlan p, String requestMode) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("assetKind", p.ref.getAssetKind()); params.put("assetRef", p.ref.value());
        params.put("slices", p.slices); params.put("periods", p.periods);
        if (requestMode != null) params.put("requestMode", requestMode);
        return JSON.toJSONString(params);
    }

    private List<String> dedup(List<String> values) {
        if (values == null) return List.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) if (value != null && !value.isBlank()) result.add(value.trim());
        return List.copyOf(result);
    }

    private String requireAShareExchangeCode(StockMarketEntity stock, String stockCode) {
        String exchangeCode = stock.getExchangeCode();
        if (exchangeCode != null && !exchangeCode.isBlank()) {
            String normalized = exchangeCode.trim().toUpperCase();
            if (!Set.of("SH", "SZ", "BJ").contains(normalized)) throw illegal("A股 exchange_code 不合法");
            return normalized;
        }
        if (stockCode == null || stockCode.isBlank()) throw illegal("A股代码不合法");
        return switch (stockCode.charAt(0)) {
            case '6' -> "SH";
            case '0', '1', '2', '3' -> "SZ";
            case '4', '8', '9' -> "BJ";
            default -> throw illegal("无法根据 A 股代码推断 exchange_code");
        };
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

    private record TaskPlan(MarketAssetRefVO ref, List<String> slices, List<String> periods,
                            String providerMarketCode, String exchangeCode) { }
    private record FundRefreshClaim(ProcessingTaskEntity task, TaskPlan plan, Map<String, String> statuses,
                                    String serverTaskId) { }
    private record StockRefreshClaim(ProcessingTaskEntity taskToDispatch, TaskPlan plan, String serverTaskId) { }
    private record SliceStateView(String status, String activeTaskId, LocalDateTime lastAttemptAt) { }
    private record SliceFact(boolean hasData, boolean stale, LocalDateTime fetchedAt) {
        private static SliceFact of(boolean hasData, LocalDateTime fetchedAt, LocalDateTime staleBefore) {
            return new SliceFact(hasData, hasData && (fetchedAt == null || fetchedAt.isBefore(staleBefore)), fetchedAt);
        }
    }
}
