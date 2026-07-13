package com.echoamoy.holdlens.server.trigger.http;

import com.echoamoy.holdlens.server.api.IAgentFundSliceRefreshService;
import com.echoamoy.holdlens.server.api.dto.FundRefreshTaskDTO;
import com.echoamoy.holdlens.server.api.request.FundSliceRefreshCallbackRequest;
import com.echoamoy.holdlens.server.api.response.Response;
import com.echoamoy.holdlens.server.cases.agent.IFundSliceRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.FundRefreshTaskResult;
import com.echoamoy.holdlens.server.cases.agent.model.FundSliceRefreshCallbackCommand;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import com.echoamoy.holdlens.server.types.exception.AppException;

import java.util.List;

@RestController
public class AgentFundSliceRefreshController implements IAgentFundSliceRefreshService {
    @Resource private IFundSliceRefreshCase fundSliceRefreshCase;
    @Value("${holdlens.agent.callback-header-value:internal}") private String callbackHeaderValue;

    @PostMapping("/internal/agent/fund-catalog-refresh/callback")
    public Response<FundRefreshTaskDTO> catalogCallback(@RequestHeader(value="X-HoldLens-Agent-Callback", required=false) String header,
                                                        @RequestBody FundSliceRefreshCallbackRequest request) {
        return callback(header, ProcessingTaskEntity.FUND_CATALOG_REFRESH, request);
    }
    @PostMapping("/internal/agent/fund-purchase-status-refresh/callback")
    public Response<FundRefreshTaskDTO> purchaseStatusCallback(@RequestHeader(value="X-HoldLens-Agent-Callback", required=false) String header,
                                                               @RequestBody FundSliceRefreshCallbackRequest request) {
        return callback(header, ProcessingTaskEntity.FUND_PURCHASE_STATUS_REFRESH, request);
    }
    @PostMapping("/internal/agent/fund-period-return-refresh/callback")
    public Response<FundRefreshTaskDTO> periodReturnCallback(@RequestHeader(value="X-HoldLens-Agent-Callback", required=false) String header,
                                                             @RequestBody FundSliceRefreshCallbackRequest request) {
        return callback(header, ProcessingTaskEntity.FUND_PERIOD_RETURN_REFRESH, request);
    }
    @PostMapping("/internal/agent/fund-top-holding-refresh/callback")
    public Response<FundRefreshTaskDTO> topHoldingCallback(@RequestHeader(value="X-HoldLens-Agent-Callback", required=false) String header,
                                                           @RequestBody FundSliceRefreshCallbackRequest request) {
        return callback(header, ProcessingTaskEntity.FUND_TOP_HOLDING_REFRESH, request);
    }

    private Response<FundRefreshTaskDTO> callback(String header, String taskType, FundSliceRefreshCallbackRequest request) {
        if (!callbackHeaderValue.equals(header)) {
            throw new AgentCallbackHttpException(HttpStatus.UNAUTHORIZED,
                    ResponseCode.ILLEGAL_PARAMETER.getCode(), "未授权 agent 回调", null);
        }
        try {
            return Response.ok(toDTO(fundSliceRefreshCase.handleCallback(taskType, toCommand(request))));
        } catch (AppException exception) {
            throw new AgentCallbackHttpException(HttpStatus.BAD_REQUEST,
                    exception.getCode(), exception.getInfo(), exception);
        } catch (RuntimeException exception) {
            throw new AgentCallbackHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    ResponseCode.UN_ERROR.getCode(), "agent 回调事务处理失败", exception);
        }
    }

    private FundSliceRefreshCallbackCommand toCommand(FundSliceRefreshCallbackRequest request) {
        if (request == null) return null;
        return FundSliceRefreshCallbackCommand.builder().schemaVersion(request.getSchemaVersion())
                .serverTaskId(request.getServerTaskId()).idempotencyKey(request.getIdempotencyKey())
                .status(request.getStatus()).generatedAt(request.getGeneratedAt())
                .funds(request.getFunds() == null ? null : request.getFunds().stream().map(this::toFund).toList())
                .refreshWarnings(request.getRefreshWarnings() == null ? null : request.getRefreshWarnings().stream()
                        .map(w -> FundSliceRefreshCallbackCommand.RefreshWarning.builder().severity(w.getSeverity())
                                .module(w.getModule()).event(w.getEvent()).message(w.getMessage()).build()).toList())
                .errorSummary(request.getErrorSummary()).build();
    }

    private FundSliceRefreshCallbackCommand.FundItem toFund(FundSliceRefreshCallbackRequest.FundItem f) {
        if (f == null) return null;
        return FundSliceRefreshCallbackCommand.FundItem.builder().fundCode(f.getFundCode()).fundName(f.getFundName())
                .fundType(f.getFundType()).pinyinAbbr(f.getPinyinAbbr()).pinyinFull(f.getPinyinFull())
                .buyStatus(f.getBuyStatus()).dailyPurchaseLimit(f.getDailyPurchaseLimit())
                .coverageStatus(f.getCoverageStatus()).returnsAsOf(f.getReturnsAsOf()).unitNav(f.getUnitNav())
                .accumulatedNav(f.getAccumulatedNav()).dailyGrowthRate(f.getDailyGrowthRate())
                .oneMonthReturn(f.getOneMonthReturn()).threeMonthsReturn(f.getThreeMonthsReturn())
                .sixMonthsReturn(f.getSixMonthsReturn()).oneYearReturn(f.getOneYearReturn()).threeYearsReturn(f.getThreeYearsReturn())
                .topHoldingsAsOf(f.getTopHoldingsAsOf()).publicHoldingsStatus(f.getPublicHoldingsStatus())
                .topHoldings(f.getTopHoldings() == null ? List.of() : f.getTopHoldings().stream().map(h ->
                        FundSliceRefreshCallbackCommand.TopHolding.builder().rankNo(h.getRankNo()).stockName(h.getStockName())
                                .stockCode(h.getStockCode()).market(h.getMarket()).holdingRatio(h.getHoldingRatio())
                                .quarterChangeType(h.getQuarterChangeType()).quarterChangeValue(h.getQuarterChangeValue()).build()).toList())
                .build();
    }

    private FundRefreshTaskDTO toDTO(FundRefreshTaskResult result) {
        return FundRefreshTaskDTO.builder().serverTaskId(result.getServerTaskId()).taskType(result.getTaskType())
                .status(result.getStatus()).errorSummary(result.getErrorSummary())
                .createTime(result.getCreateTime()).updateTime(result.getUpdateTime()).build();
    }
}
