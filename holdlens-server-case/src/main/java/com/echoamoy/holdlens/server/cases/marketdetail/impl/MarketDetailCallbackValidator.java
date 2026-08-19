package com.echoamoy.holdlens.server.cases.marketdetail.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.echoamoy.holdlens.server.cases.marketdetail.model.MarketDetailCommand;
import com.echoamoy.holdlens.server.domain.marketasset.model.valobj.MarketAssetRefVO;
import com.echoamoy.holdlens.server.domain.processing.adapter.repository.IProcessingTaskRepository;
import com.echoamoy.holdlens.server.domain.processing.model.entity.ProcessingTaskEntity;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MarketDetailCallbackValidator {

    private static final String RESULT_SCHEMA = "market-detail-data-refresh-result/v1";

    @Resource
    private IProcessingTaskRepository processingTaskRepository;

    CallbackPlan validate(MarketDetailCommand.Callback callback) {
        if (callback == null || callback.getServerTaskId() == null) throw illegal("回调缺少任务标识");
        ProcessingTaskEntity task = processingTaskRepository.queryTask(callback.getServerTaskId());
        if (task == null || !ProcessingTaskEntity.MARKET_DETAIL_DATA_REFRESH.equals(task.getTaskType())) {
            throw illegal("未知任务");
        }
        if (!RESULT_SCHEMA.equals(callback.getSchemaVersion())) throw illegal("回调 schema 不支持");
        if (!("succeeded".equals(callback.getStatus()) || "partial_failed".equals(callback.getStatus())
                || "failed".equals(callback.getStatus()))) {
            throw illegal("回调状态不支持");
        }
        if (!(callback.getServerTaskId() + ":result:1").equals(callback.getIdempotencyKey())) {
            throw illegal("幂等键不合法");
        }

        JSONObject params = JSON.parseObject(task.getTaskParamsJson());
        MarketAssetRefVO ref;
        try {
            ref = MarketAssetRefVO.parse(params.getString("assetKind"), params.getString("assetRef"));
        } catch (IllegalArgumentException exception) {
            throw illegal("任务引用不合法");
        }
        if (!ref.value().equals(callback.getAssetRef()) || !ref.getAssetKind().equals(callback.getAssetKind())) {
            throw illegal("回调资产与任务不一致");
        }

        List<String> slices = params.getJSONArray("slices").toJavaList(String.class);
        validateSliceResults(callback.getSliceResults(), slices, callback.getStatus());
        if (callback.getFundNavHistory() != null && callback.getFundNavHistory().getPoints() != null
                && !callback.getFundNavHistory().getPoints().isEmpty() && !slices.contains("nav_history")
                || callback.getFundPeriodPerformance() != null && callback.getFundPeriodPerformance().getRows() != null
                && !callback.getFundPeriodPerformance().getRows().isEmpty() && !slices.contains("period_performance")
                || callback.getStockPriceHistories() != null && !callback.getStockPriceHistories().isEmpty()
                && !slices.contains("price_history")
                || callback.getStockCompanyProfile() != null && !slices.contains("company_profile")) {
            throw illegal("回调包含任务未请求的 slice");
        }
        return new CallbackPlan(task, ref, slices, params.getJSONArray("periods").toJavaList(String.class));
    }

    String sliceResultStatus(MarketDetailCommand.Callback command, String slice) {
        if (command.getSliceResults() == null) return null;
        return command.getSliceResults().stream().filter(result -> slice.equals(result.getSlice()))
                .map(MarketDetailCommand.SliceResult::getStatus).findFirst().orElse(null);
    }

    private void validateSliceResults(List<MarketDetailCommand.SliceResult> results, List<String> requestedSlices,
                                      String overallStatus) {
        if (results == null) return;
        Map<String, String> statuses = new LinkedHashMap<>();
        for (MarketDetailCommand.SliceResult result : results) {
            if (result == null || !requestedSlices.contains(result.getSlice())
                    || !Set.of("available", "empty", "failed").contains(result.getStatus())
                    || statuses.putIfAbsent(result.getSlice(), result.getStatus()) != null) {
                throw illegal("callback slice_results 不合法");
            }
        }
        if (statuses.size() != requestedSlices.size() || !statuses.keySet().containsAll(requestedSlices)) {
            throw illegal("callback slice_results 必须覆盖全部请求 slice");
        }
        long failures = statuses.values().stream().filter("failed"::equals).count();
        if ("succeeded".equals(overallStatus) && failures > 0
                || "failed".equals(overallStatus) && failures != statuses.size()
                || "partial_failed".equals(overallStatus) && (failures == 0 || failures == statuses.size())) {
            throw illegal("callback 整体状态与 slice_results 不一致");
        }
    }

    private AppException illegal(String message) {
        return new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), message);
    }
}

final class CallbackPlan {
    final ProcessingTaskEntity task;
    final MarketAssetRefVO ref;
    final List<String> slices;
    final List<String> periods;

    CallbackPlan(ProcessingTaskEntity task, MarketAssetRefVO ref, List<String> slices, List<String> periods) {
        this.task = task;
        this.ref = ref;
        this.slices = slices;
        this.periods = periods;
    }
}
