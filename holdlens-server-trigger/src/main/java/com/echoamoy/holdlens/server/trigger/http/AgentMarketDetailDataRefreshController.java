package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IMarketDetailService;
import com.echoamoy.holdlens.server.api.dto.MarketDetailDTO;
import com.echoamoy.holdlens.server.api.request.MarketDetailRefreshRequest;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.marketdetail.IMarketDetailCase;
import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailCommand;
import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailResult;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AgentMarketDetailDataRefreshController implements IMarketDetailService {

    @Resource private IMarketDetailCase marketDetailCase;
    @Value("${holdlens.agent.callback-header-value}") private String callbackHeaderValue;

    @Override
    @PostMapping("/api/agent/market-detail-data-refresh/tasks")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Response<MarketDetailDTO.Task> createTask(@RequestBody MarketDetailRefreshRequest.Create request) {
        MarketDetailResult.Task result = marketDetailCase.createAndDispatch(MarketDetailCommand.CreateTask.builder()
                .assetKind(request == null ? null : request.getAssetKind()).assetRef(request == null ? null : request.getAssetRef())
                .slices(request == null ? null : request.getSlices()).periods(request == null ? null : request.getPeriods()).build());
        MarketDetailDTO.Task task = toTask(result);
        if ("dispatched".equals(result.getStatus()) || "running".equals(result.getStatus())) {
            task.setStatus("accepted");
        }
        return Response.<MarketDetailDTO.Task>builder().code("0000").info("accepted").data(task).build();
    }

    @Override
    @PostMapping("/internal/agent/market-detail-data-refresh/callback")
    public Response<MarketDetailDTO.Task> callback(
            @RequestHeader(value = "X-HoldLens-Agent-Callback", required = false) String callbackHeader,
            @RequestHeader(value = "X-HoldLens-Idempotency-Key", required = false) String idempotencyHeader,
            @RequestBody MarketDetailRefreshRequest.Callback request) {
        if (!callbackHeaderValue.equals(callbackHeader)) {
            throw new AgentCallbackHttpException(HttpStatus.UNAUTHORIZED,
                    ResponseCode.ILLEGAL_PARAMETER.getCode(), "未授权 agent 回调", null);
        }
        if (request == null || idempotencyHeader == null || !idempotencyHeader.equals(request.getIdempotencyKey())) {
            throw new AgentCallbackHttpException(HttpStatus.BAD_REQUEST,
                    ResponseCode.ILLEGAL_PARAMETER.getCode(), "回调幂等头与请求体不一致", null);
        }
        return Response.ok(toTask(marketDetailCase.handleCallback(toCommand(request))));
    }

    @Override
    @GetMapping("/api/funds/{fundCode}/nav-history")
    public Response<MarketDetailDTO.FundNavHistory> queryFundNavHistory(@PathVariable("fundCode") String fundCode,
                                                                        @RequestParam("period") String period) {
        MarketDetailResult.FundNavHistory r = marketDetailCase.queryFundNavHistory(fundCode, period);
        return Response.ok(MarketDetailDTO.FundNavHistory.builder().fundCode(r.getFundCode()).period(r.getPeriod())
                .asOf(r.getAsOf()).points(r.getPoints().stream().map(p -> MarketDetailDTO.FundNavPoint.builder()
                        .navDate(p.getNavDate()).unitNav(p.getUnitNav()).accumulatedNav(p.getAccumulatedNav())
                        .dailyGrowthRate(p.getDailyGrowthRate()).build()).toList()).build());
    }

    @Override
    @GetMapping("/api/stocks/price-history")
    public Response<MarketDetailDTO.StockPriceHistory> queryStockPriceHistory(@RequestParam("assetRef") String assetRef,
                                                                              @RequestParam("period") String period) {
        MarketDetailResult.StockPriceHistory r = marketDetailCase.queryStockPriceHistory(assetRef, period);
        return Response.ok(MarketDetailDTO.StockPriceHistory.builder().assetRef(r.getAssetRef()).period(r.getPeriod())
                .granularity(r.getGranularity()).currency(r.getCurrency()).asOf(r.getAsOf())
                .points(r.getPoints().stream().map(p -> MarketDetailDTO.StockBar.builder().barTime(p.getBarTime())
                        .open(p.getOpen()).high(p.getHigh()).low(p.getLow()).close(p.getClose()).volume(p.getVolume()).build())
                        .toList()).build());
    }

    @Override
    @GetMapping("/api/stocks/company-profile")
    public Response<MarketDetailDTO.StockCompanyProfile> queryStockCompanyProfile(@RequestParam("assetRef") String assetRef) {
        MarketDetailResult.StockCompanyProfile r = marketDetailCase.queryStockCompanyProfile(assetRef);
        return Response.ok(MarketDetailDTO.StockCompanyProfile.builder().assetRef(r.getAssetRef())
                .companyName(r.getCompanyName()).industry(r.getIndustry()).businessSummary(r.getBusinessSummary())
                .companyProfile(r.getCompanyProfile()).website(r.getWebsite()).asOf(r.getAsOf()).build());
    }

    private MarketDetailCommand.Callback toCommand(MarketDetailRefreshRequest.Callback r) {
        return MarketDetailCommand.Callback.builder().schemaVersion(r.getSchemaVersion()).serverTaskId(r.getServerTaskId())
                .idempotencyKey(r.getIdempotencyKey()).status(r.getStatus()).generatedAt(r.getGeneratedAt())
                .assetKind(r.getAssetKind()).assetRef(r.getAssetRef())
                .fundNavHistory(toFundNav(r.getFundNavHistory()))
                .stockPriceHistories(r.getStockPriceHistories() == null ? null : r.getStockPriceHistories().stream()
                        .map(h -> MarketDetailCommand.StockPriceHistory.builder().period(h.getPeriod())
                                .granularity(h.getGranularity()).currency(h.getCurrency())
                                .bars(h.getBars() == null ? null : h.getBars().stream().map(b -> MarketDetailCommand.StockBar.builder()
                                        .barTime(b.getBarTime()).open(b.getOpen()).high(b.getHigh()).low(b.getLow())
                                        .close(b.getClose()).volume(b.getVolume()).build()).toList()).build()).toList())
                .stockCompanyProfile(toProfile(r.getStockCompanyProfile()))
                .refreshWarnings(r.getRefreshWarnings() == null ? null : r.getRefreshWarnings().stream()
                        .map(w -> MarketDetailCommand.RefreshWarning.builder().module(w.getModule()).event(w.getEvent())
                                .message(w.getMessage()).severity(w.getSeverity()).build()).toList())
                .errorSummary(r.getErrorSummary()).build();
    }

    private MarketDetailCommand.FundNavHistory toFundNav(MarketDetailRefreshRequest.FundNavHistory h) {
        if (h == null) return null;
        return MarketDetailCommand.FundNavHistory.builder().fundCode(h.getFundCode())
                .points(h.getPoints() == null ? List.of() : h.getPoints().stream().map(p -> MarketDetailCommand.FundNavPoint.builder()
                        .navDate(p.getNavDate()).unitNav(p.getUnitNav()).accumulatedNav(p.getAccumulatedNav())
                        .dailyGrowthRate(p.getDailyGrowthRate()).build()).toList()).build();
    }

    private MarketDetailCommand.StockCompanyProfile toProfile(MarketDetailRefreshRequest.StockCompanyProfile p) {
        if (p == null) return null;
        return MarketDetailCommand.StockCompanyProfile.builder().companyName(p.getCompanyName()).industry(p.getIndustry())
                .businessSummary(p.getBusinessSummary()).companyProfile(p.getCompanyProfile()).website(p.getWebsite())
                .sourceAsOf(p.getSourceAsOf()).build();
    }

    private MarketDetailDTO.Task toTask(MarketDetailResult.Task r) {
        return MarketDetailDTO.Task.builder().serverTaskId(r.getServerTaskId()).taskType(r.getTaskType()).status(r.getStatus()).build();
    }
}
