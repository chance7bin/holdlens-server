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
public class ProcessingLogPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 处理日志ID */
    private Long id;

    /** 来源引用ID，当前对应 processing_task.server_task_id */
    private String sourceRefId;

    /** 处理模块 */
    private String module;

    /** 处理事件 */
    private String event;

    /** 安全诊断消息 */
    private String message;

    /** 级别：info/warning/error */
    private String severity;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
