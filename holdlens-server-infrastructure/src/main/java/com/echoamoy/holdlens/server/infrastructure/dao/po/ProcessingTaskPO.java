package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingTaskPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 处理任务ID */
    private Long id;

    /** server任务标识 */
    private String serverTaskId;

    /** 任务类型：fund_detail_refresh */
    private String taskType;

    /** 基金代码数量 */
    private Integer fundCodeCount;

    /** 来源类型 */
    private String sourceType;

    /** 来源引用ID */
    private String sourceRefId;

    /** 状态：created/dispatched/running/succeeded/partial_failed/failed/dispatch_failed/callback_failed */
    private String status;

    /** agent任务引用 */
    private String agentTaskRef;

    /** 安全错误摘要 */
    private String errorSummary;

    /** 回调诊断状态 */
    private String callbackDiagnosticStatus;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
