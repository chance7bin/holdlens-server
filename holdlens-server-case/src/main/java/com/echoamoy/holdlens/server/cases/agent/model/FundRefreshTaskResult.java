package com.echoamoy.holdlens.server.cases.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundRefreshTaskResult {

    private String serverTaskId;

    private String taskType;

    private String status;

    private String errorSummary;

    private Date createTime;

    private Date updateTime;

}
