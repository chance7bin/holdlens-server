package com.echoamoy.holdlens.server.api.dto;

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
public class FundRefreshTaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String serverTaskId;

    private String taskType;

    private String status;

    private Integer fundCodeCount;

    private String sourceType;

    private String sourceRefId;

    private String agentTaskRef;

    private String errorSummary;

    private String callbackDiagnosticStatus;

    private Date createTime;

    private Date updateTime;

}
