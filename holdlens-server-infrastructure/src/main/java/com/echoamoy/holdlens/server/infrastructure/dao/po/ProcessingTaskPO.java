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

    /** 任务类型 */
    private String taskType;

    /** 安全任务参数摘要JSON */
    private String taskParamsJson;

    /** 状态：created/dispatched/running/succeeded/partial_failed/failed/dispatch_failed/callback_failed */
    private String status;

    /** 安全错误摘要 */
    private String errorSummary;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
