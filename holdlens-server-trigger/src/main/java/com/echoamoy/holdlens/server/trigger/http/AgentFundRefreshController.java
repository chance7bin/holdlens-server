package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IAgentFundRefreshService;
import com.echoamoy.holdlens.server.api.dto.FundRefreshTaskDTO;
import com.echoamoy.holdlens.server.api.request.AgentFundRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.AShareMarketRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.AShareMarketRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.request.FundRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.request.USStockMarketRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.USStockMarketRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.AgentFundRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping
public class AgentFundRefreshController implements IAgentFundRefreshService {

    @Resource
    private IAgentFundRefreshCase agentFundRefreshCase;

    @Value("${holdlens.agent.callback-header-value:internal}")
    private String callbackHeaderValue;

    @PostMapping("/api/agent/fund-detail-refresh/tasks")
    @Override
    public Response<FundRefreshTaskDTO> createTask(@Valid @RequestBody FundRefreshCreateRequest request) {
        return Response.ok(toTaskDTO(agentFundRefreshCase.createAndDispatch(toCreateCommand(request))));
    }

    @GetMapping("/api/agent/fund-detail-refresh/tasks/{serverTaskId}")
    @Override
    public Response<FundRefreshTaskDTO> queryTask(@PathVariable String serverTaskId) {
        return Response.ok(toTaskDTO(agentFundRefreshCase.queryTask(serverTaskId)));
    }

    @PostMapping("/api/agent/a-share-market-refresh/tasks")
    @Override
    public Response<FundRefreshTaskDTO> createAShareMarketTask(@RequestBody(required = false) AShareMarketRefreshCreateRequest request) {
        return Response.ok(toTaskDTO(agentFundRefreshCase.createAndDispatchAShareMarket(toAShareMarketCreateCommand(request))));
    }

    @PostMapping("/api/agent/us-stock-market-refresh/tasks")
    @Override
    public Response<FundRefreshTaskDTO> createUSStockMarketTask(@RequestBody(required = false) USStockMarketRefreshCreateRequest request) {
        return Response.ok(toTaskDTO(agentFundRefreshCase.createAndDispatchUSStockMarket(toUSStockMarketCreateCommand(request))));
    }

    @PostMapping("/internal/agent/fund-detail-refresh/callback")
    @Override
    public Response<FundRefreshTaskDTO> callback(@RequestHeader(value = "X-HoldLens-Agent-Callback", required = false) String callbackHeader,
                                                 @RequestBody AgentFundRefreshCallbackRequest request) {
        if (!callbackHeaderValue.equals(callbackHeader)) {
            return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未授权 agent 回调");
        }
        return Response.ok(toTaskDTO(agentFundRefreshCase.handleCallback(toCallbackCommand(request))));
    }

    @PostMapping("/internal/agent/a-share-market-refresh/callback")
    @Override
    public Response<FundRefreshTaskDTO> aShareMarketCallback(@RequestHeader(value = "X-HoldLens-Agent-Callback", required = false) String callbackHeader,
                                                             @RequestBody AShareMarketRefreshCallbackRequest request) {
        if (!callbackHeaderValue.equals(callbackHeader)) {
            return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未授权 agent 回调");
        }
        return Response.ok(toTaskDTO(agentFundRefreshCase.handleAShareMarketCallback(toAShareMarketCallbackCommand(request))));
    }

    @PostMapping("/internal/agent/us-stock-market-refresh/callback")
    @Override
    public Response<FundRefreshTaskDTO> usStockMarketCallback(@RequestHeader(value = "X-HoldLens-Agent-Callback", required = false) String callbackHeader,
                                                              @RequestBody USStockMarketRefreshCallbackRequest request) {
        if (!callbackHeaderValue.equals(callbackHeader)) {
            return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未授权 agent 回调");
        }
        return Response.ok(toTaskDTO(agentFundRefreshCase.handleUSStockMarketCallback(toUSStockMarketCallbackCommand(request))));
    }

    private FundRefreshCreateCommand toCreateCommand(FundRefreshCreateRequest request) {
        if (request == null) {
            return null;
        }
        return FundRefreshCreateCommand.builder()
                .fundCodes(request.getFundCodes())
                .build();
    }

    private AShareMarketRefreshCreateCommand toAShareMarketCreateCommand(AShareMarketRefreshCreateRequest request) {
        if (request == null) {
            return null;
        }
        return AShareMarketRefreshCreateCommand.builder()
                .trigger(request.getTrigger())
                .build();
    }

    private USStockMarketRefreshCreateCommand toUSStockMarketCreateCommand(USStockMarketRefreshCreateRequest request) {
        if (request == null) {
            return null;
        }
        return USStockMarketRefreshCreateCommand.builder()
                .trigger(request.getTrigger())
                .build();
    }

    private AgentFundRefreshCallbackCommand toCallbackCommand(AgentFundRefreshCallbackRequest request) {
        if (request == null) {
            return null;
        }
        return AgentFundRefreshCallbackCommand.builder()
                .schemaVersion(request.getSchemaVersion())
                .serverTaskId(request.getServerTaskId())
                .idempotencyKey(request.getIdempotencyKey())
                .status(request.getStatus())
                .generatedAt(request.getGeneratedAt())
                .funds(toFundDetails(request.getFunds()))
                .refreshWarnings(toRefreshWarnings(request.getRefreshWarnings()))
                .errorSummary(request.getErrorSummary())
                .build();
    }

    private List<AgentFundRefreshCallbackCommand.FundDetail> toFundDetails(List<AgentFundRefreshCallbackRequest.FundDetail> funds) {
        if (funds == null) {
            return null;
        }
        return funds.stream()
                .map(fund -> fund == null ? null : AgentFundRefreshCallbackCommand.FundDetail.builder()
                        .fundCode(fund.getFundCode())
                        .fundName(fund.getFundName())
                        .buyStatus(fund.getBuyStatus())
                        .dailyPurchaseLimit(fund.getDailyPurchaseLimit())
                        .returnsAsOf(fund.getReturnsAsOf())
                        .topHoldingsAsOf(fund.getTopHoldingsAsOf())
                        .publicHoldingsStatus(fund.getPublicHoldingsStatus())
                        .oneMonthReturn(fund.getOneMonthReturn())
                        .threeMonthsReturn(fund.getThreeMonthsReturn())
                        .sixMonthsReturn(fund.getSixMonthsReturn())
                        .oneYearReturn(fund.getOneYearReturn())
                        .threeYearsReturn(fund.getThreeYearsReturn())
                        .topHoldings(toTopHoldings(fund.getTopHoldings()))
                        .build())
                .toList();
    }

    private List<AgentFundRefreshCallbackCommand.TopHolding> toTopHoldings(List<AgentFundRefreshCallbackRequest.TopHolding> topHoldings) {
        if (topHoldings == null) {
            return null;
        }
        return topHoldings.stream()
                .map(topHolding -> topHolding == null ? null : AgentFundRefreshCallbackCommand.TopHolding.builder()
                        .rankNo(topHolding.getRankNo())
                        .stockName(topHolding.getStockName())
                        .stockCode(topHolding.getStockCode())
                        .market(topHolding.getMarket())
                        .holdingRatio(topHolding.getHoldingRatio())
                        .quarterChangeType(topHolding.getQuarterChangeType())
                        .quarterChangeValue(topHolding.getQuarterChangeValue())
                        .build())
                .toList();
    }

    private List<AgentFundRefreshCallbackCommand.RefreshWarning> toRefreshWarnings(List<AgentFundRefreshCallbackRequest.RefreshWarning> warnings) {
        if (warnings == null) {
            return null;
        }
        return warnings.stream()
                .map(warning -> warning == null ? null : AgentFundRefreshCallbackCommand.RefreshWarning.builder()
                        .module(warning.getModule())
                        .event(warning.getEvent())
                        .message(warning.getMessage())
                        .severity(warning.getSeverity())
                        .build())
                .toList();
    }

    private AShareMarketRefreshCallbackCommand toAShareMarketCallbackCommand(AShareMarketRefreshCallbackRequest request) {
        if (request == null) {
            return null;
        }
        return AShareMarketRefreshCallbackCommand.builder()
                .schemaVersion(request.getSchemaVersion())
                .serverTaskId(request.getServerTaskId())
                .idempotencyKey(request.getIdempotencyKey())
                .status(request.getStatus())
                .generatedAt(request.getGeneratedAt())
                .market(request.getMarket())
                .stocks(toStockMarkets(request.getStocks()))
                .refreshWarnings(toAShareMarketWarnings(request.getRefreshWarnings()))
                .errorSummary(request.getErrorSummary())
                .build();
    }

    private List<AShareMarketRefreshCallbackCommand.StockMarket> toStockMarkets(List<AShareMarketRefreshCallbackRequest.StockMarket> stocks) {
        if (stocks == null) {
            return null;
        }
        return stocks.stream()
                .map(stock -> stock == null ? null : AShareMarketRefreshCallbackCommand.StockMarket.builder()
                        .stockCode(stock.getStockCode())
                        .stockName(stock.getStockName())
                        .market(stock.getMarket())
                        .status(stock.getStatus())
                        .exchangeCode(stock.getExchangeCode())
                        .providerMarketCode(stock.getProviderMarketCode())
                        .currency(stock.getCurrency())
                        .volumeUnit(stock.getVolumeUnit())
                        .latestPrice(stock.getLatestPrice())
                        .changePercent(stock.getChangePercent())
                        .changeAmount(stock.getChangeAmount())
                        .volume(stock.getVolume())
                        .turnoverAmount(stock.getTurnoverAmount())
                        .amplitude(stock.getAmplitude())
                        .highPrice(stock.getHighPrice())
                        .lowPrice(stock.getLowPrice())
                        .openPrice(stock.getOpenPrice())
                        .previousClose(stock.getPreviousClose())
                        .volumeRatio(stock.getVolumeRatio())
                        .turnoverRate(stock.getTurnoverRate())
                        .peDynamic(stock.getPeDynamic())
                        .pbRatio(stock.getPbRatio())
                        .totalMarketValue(stock.getTotalMarketValue())
                        .circulatingMarketValue(stock.getCirculatingMarketValue())
                        .speed(stock.getSpeed())
                        .fiveMinuteChange(stock.getFiveMinuteChange())
                        .sixtyDayChangePercent(stock.getSixtyDayChangePercent())
                        .yearToDateChangePercent(stock.getYearToDateChangePercent())
                        .refreshedAt(stock.getRefreshedAt())
                        .build())
                .toList();
    }

    private List<AShareMarketRefreshCallbackCommand.RefreshWarning> toAShareMarketWarnings(List<AShareMarketRefreshCallbackRequest.RefreshWarning> warnings) {
        if (warnings == null) {
            return null;
        }
        return warnings.stream()
                .map(warning -> warning == null ? null : AShareMarketRefreshCallbackCommand.RefreshWarning.builder()
                        .module(warning.getModule())
                        .event(warning.getEvent())
                        .message(warning.getMessage())
                        .severity(warning.getSeverity())
                        .build())
                .toList();
    }

    private USStockMarketRefreshCallbackCommand toUSStockMarketCallbackCommand(USStockMarketRefreshCallbackRequest request) {
        if (request == null) {
            return null;
        }
        return USStockMarketRefreshCallbackCommand.builder()
                .schemaVersion(request.getSchemaVersion())
                .serverTaskId(request.getServerTaskId())
                .idempotencyKey(request.getIdempotencyKey())
                .status(request.getStatus())
                .generatedAt(request.getGeneratedAt())
                .market(request.getMarket())
                .stocks(toUSStockMarkets(request.getStocks()))
                .refreshWarnings(toUSStockMarketWarnings(request.getRefreshWarnings()))
                .errorSummary(request.getErrorSummary())
                .build();
    }

    private List<USStockMarketRefreshCallbackCommand.StockMarket> toUSStockMarkets(List<USStockMarketRefreshCallbackRequest.StockMarket> stocks) {
        if (stocks == null) {
            return null;
        }
        return stocks.stream()
                .map(stock -> stock == null ? null : USStockMarketRefreshCallbackCommand.StockMarket.builder()
                        .stockCode(stock.getStockCode())
                        .stockName(stock.getStockName())
                        .market(stock.getMarket())
                        .status(stock.getStatus())
                        .exchangeCode(stock.getExchangeCode())
                        .providerMarketCode(stock.getProviderMarketCode())
                        .secid(stock.getSecid())
                        .currency(stock.getCurrency())
                        .volumeUnit(stock.getVolumeUnit())
                        .latestPrice(stock.getLatestPrice())
                        .changePercent(stock.getChangePercent())
                        .changeAmount(stock.getChangeAmount())
                        .volume(stock.getVolume())
                        .turnoverAmount(stock.getTurnoverAmount())
                        .amplitude(stock.getAmplitude())
                        .highPrice(stock.getHighPrice())
                        .lowPrice(stock.getLowPrice())
                        .openPrice(stock.getOpenPrice())
                        .previousClose(stock.getPreviousClose())
                        .volumeRatio(stock.getVolumeRatio())
                        .turnoverRate(stock.getTurnoverRate())
                        .peRatio(stock.getPeRatio())
                        .pbRatio(stock.getPbRatio())
                        .totalMarketValue(stock.getTotalMarketValue())
                        .circulatingMarketValue(stock.getCirculatingMarketValue())
                        .speed(stock.getSpeed())
                        .fiveMinuteChange(stock.getFiveMinuteChange())
                        .sixtyDayChangePercent(stock.getSixtyDayChangePercent())
                        .yearToDateChangePercent(stock.getYearToDateChangePercent())
                        .listingDate(stock.getListingDate())
                        .refreshedAt(stock.getRefreshedAt())
                        .build())
                .toList();
    }

    private List<USStockMarketRefreshCallbackCommand.RefreshWarning> toUSStockMarketWarnings(List<USStockMarketRefreshCallbackRequest.RefreshWarning> warnings) {
        if (warnings == null) {
            return null;
        }
        return warnings.stream()
                .map(warning -> warning == null ? null : USStockMarketRefreshCallbackCommand.RefreshWarning.builder()
                        .module(warning.getModule())
                        .event(warning.getEvent())
                        .message(warning.getMessage())
                        .severity(warning.getSeverity())
                        .build())
                .toList();
    }

    private FundRefreshTaskDTO toTaskDTO(FundRefreshTaskResult result) {
        if (result == null) {
            return null;
        }
        return FundRefreshTaskDTO.builder()
                .serverTaskId(result.getServerTaskId())
                .taskType(result.getTaskType())
                .status(result.getStatus())
                .errorSummary(result.getErrorSummary())
                .createTime(result.getCreateTime())
                .updateTime(result.getUpdateTime())
                .build();
    }

}
