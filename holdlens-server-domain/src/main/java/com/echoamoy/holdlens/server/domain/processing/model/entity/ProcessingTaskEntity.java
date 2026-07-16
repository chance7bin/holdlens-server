package com.echoamoy.holdlens.server.domain.processing.model.entity;

import com.echoamoy.holdlens.server.domain.processing.model.valobj.ProcessingTaskStatusEnumVO;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingTaskEntity {

    public static final String FUND_CATALOG_REFRESH = "fund_catalog_refresh";
    public static final String FUND_PURCHASE_STATUS_REFRESH = "fund_purchase_status_refresh";
    public static final String FUND_PERIOD_RETURN_REFRESH = "fund_period_return_refresh";
    public static final String FUND_TOP_HOLDING_REFRESH = "fund_top_holding_refresh";
    public static final String FUND_ASSET_ALLOCATION_REFRESH = "fund_asset_allocation_refresh";
    public static final String A_SHARE_MARKET_REFRESH = "a_share_market_refresh";
    public static final String US_STOCK_MARKET_REFRESH = "us_stock_market_refresh";

    public static boolean isFundSliceRefresh(String taskType) {
        return FUND_CATALOG_REFRESH.equals(taskType)
                || FUND_PURCHASE_STATUS_REFRESH.equals(taskType)
                || FUND_PERIOD_RETURN_REFRESH.equals(taskType)
                || FUND_TOP_HOLDING_REFRESH.equals(taskType)
                || FUND_ASSET_ALLOCATION_REFRESH.equals(taskType);
    }

    private Long id;
    private String serverTaskId;
    private String taskType;
    private String taskParamsJson;
    private ProcessingTaskStatusEnumVO status;
    private String errorSummary;
    private Date createTime;
    private Date updateTime;

    public void transitTo(ProcessingTaskStatusEnumVO targetStatus, String nextErrorSummary) {
        ProcessingTaskStatusEnumVO current = status == null ? ProcessingTaskStatusEnumVO.CREATED : status;
        if (!current.canTransitTo(targetStatus)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "任务状态不允许从 " + current.getCode() + " 流转到 " + targetStatus.getCode());
        }
        this.status = targetStatus;
        if (nextErrorSummary != null) {
            this.errorSummary = nextErrorSummary;
        }
    }

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }

}
