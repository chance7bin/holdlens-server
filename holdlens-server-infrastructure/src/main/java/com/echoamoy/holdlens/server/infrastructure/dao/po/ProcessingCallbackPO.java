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
public class ProcessingCallbackPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 回调幂等记录ID */
    private Long id;

    /** server任务标识 */
    private String serverTaskId;

    /** 幂等键 */
    private String idempotencyKey;

    /** 回调状态 */
    private String callbackStatus;

    /** 处理状态：created/processing/processed/failed */
    private String processStatus;

    /** 安全错误摘要 */
    private String errorSummary;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
