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

    public static final String FUND_DETAIL_REFRESH = "fund_detail_refresh";

    private Long id;
    private String serverTaskId;
    private String taskType;
    private Integer fundCodeCount;
    private String sourceType;
    private String sourceRefId;
    private ProcessingTaskStatusEnumVO status;
    private String agentTaskRef;
    private String errorSummary;
    private String callbackDiagnosticStatus;
    private Date createTime;
    private Date updateTime;

    public void transitTo(ProcessingTaskStatusEnumVO targetStatus, String nextAgentTaskRef, String nextErrorSummary) {
        ProcessingTaskStatusEnumVO current = status == null ? ProcessingTaskStatusEnumVO.CREATED : status;
        if (!current.canTransitTo(targetStatus)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(),
                    "任务状态不允许从 " + current.getCode() + " 流转到 " + targetStatus.getCode());
        }
        this.status = targetStatus;
        if (nextAgentTaskRef != null) {
            this.agentTaskRef = nextAgentTaskRef;
        }
        if (nextErrorSummary != null) {
            this.errorSummary = nextErrorSummary;
        }
    }

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }

}
