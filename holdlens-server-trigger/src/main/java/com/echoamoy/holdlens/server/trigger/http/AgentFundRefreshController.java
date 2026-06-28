package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IAgentFundRefreshService;
import com.echoamoy.holdlens.server.api.dto.FundRefreshTaskDTO;
import com.echoamoy.holdlens.server.api.request.AgentFundRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.AgentStockQuoteRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.request.FundRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.request.StockQuoteRefreshCreateRequest;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.AgentFundRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.AgentStockQuoteRefreshCallbackCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.StockQuoteRefreshCreateCommand;
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

    @PostMapping("/api/agent/stock-quote-refresh/tasks")
    @Override
    public Response<FundRefreshTaskDTO> createStockQuoteTask(@Valid @RequestBody StockQuoteRefreshCreateRequest request) {
        return Response.ok(toTaskDTO(agentFundRefreshCase.createAndDispatchStockQuotes(toStockQuoteCreateCommand(request))));
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

    @PostMapping("/internal/agent/stock-quote-refresh/callback")
    @Override
    public Response<FundRefreshTaskDTO> stockQuoteCallback(@RequestHeader(value = "X-HoldLens-Agent-Callback", required = false) String callbackHeader,
                                                           @RequestBody AgentStockQuoteRefreshCallbackRequest request) {
        if (!callbackHeaderValue.equals(callbackHeader)) {
            return Response.fail(ResponseCode.ILLEGAL_PARAMETER.getCode(), "未授权 agent 回调");
        }
        return Response.ok(toTaskDTO(agentFundRefreshCase.handleStockQuoteCallback(toStockQuoteCallbackCommand(request))));
    }

    private FundRefreshCreateCommand toCreateCommand(FundRefreshCreateRequest request) {
        if (request == null) {
            return null;
        }
        return FundRefreshCreateCommand.builder()
                .fundCodes(request.getFundCodes())
                .build();
    }

    private StockQuoteRefreshCreateCommand toStockQuoteCreateCommand(StockQuoteRefreshCreateRequest request) {
        if (request == null) {
            return null;
        }
        return StockQuoteRefreshCreateCommand.builder()
                .stocks(toStockQuoteTargets(request.getStocks()))
                .build();
    }

    private List<StockQuoteRefreshCreateCommand.Stock> toStockQuoteTargets(List<StockQuoteRefreshCreateRequest.Stock> stocks) {
        if (stocks == null) {
            return null;
        }
        return stocks.stream()
                .map(stock -> stock == null ? null : StockQuoteRefreshCreateCommand.Stock.builder()
                        .stockCode(stock.getStockCode())
                        .market(stock.getMarket())
                        .build())
                .toList();
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

    private AgentStockQuoteRefreshCallbackCommand toStockQuoteCallbackCommand(AgentStockQuoteRefreshCallbackRequest request) {
        if (request == null) {
            return null;
        }
        return AgentStockQuoteRefreshCallbackCommand.builder()
                .schemaVersion(request.getSchemaVersion())
                .serverTaskId(request.getServerTaskId())
                .idempotencyKey(request.getIdempotencyKey())
                .status(request.getStatus())
                .generatedAt(request.getGeneratedAt())
                .quotes(toStockQuotes(request.getQuotes()))
                .refreshWarnings(toStockRefreshWarnings(request.getRefreshWarnings()))
                .errorSummary(request.getErrorSummary())
                .build();
    }

    private List<AgentStockQuoteRefreshCallbackCommand.StockQuote> toStockQuotes(List<AgentStockQuoteRefreshCallbackRequest.StockQuote> quotes) {
        if (quotes == null) {
            return null;
        }
        return quotes.stream()
                .map(quote -> quote == null ? null : AgentStockQuoteRefreshCallbackCommand.StockQuote.builder()
                        .stockCode(quote.getStockCode())
                        .market(quote.getMarket())
                        .stockName(quote.getStockName())
                        .tradeDate(quote.getTradeDate())
                        .dailyReturn(quote.getDailyReturn())
                        .quoteTime(quote.getQuoteTime())
                        .build())
                .toList();
    }

    private List<AgentStockQuoteRefreshCallbackCommand.RefreshWarning> toStockRefreshWarnings(List<AgentStockQuoteRefreshCallbackRequest.RefreshWarning> warnings) {
        if (warnings == null) {
            return null;
        }
        return warnings.stream()
                .map(warning -> warning == null ? null : AgentStockQuoteRefreshCallbackCommand.RefreshWarning.builder()
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
