package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IAgentFundRefreshService;
import com.echoamoy.holdlens.server.api.dto.FundRefreshTaskDTO;
import com.echoamoy.holdlens.server.api.request.AShareMarketRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.AShareMarketRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.request.USStockMarketRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.USStockMarketRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping
public class AgentFundRefreshController implements IAgentFundRefreshService {

    @Resource
    private IAgentFundRefreshCase agentFundRefreshCase;

    @Value("${holdlens.agent.callback-header-value}")
    private String callbackHeaderValue;

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
