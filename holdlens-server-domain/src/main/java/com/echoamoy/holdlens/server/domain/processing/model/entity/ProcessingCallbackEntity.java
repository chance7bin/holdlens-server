package com.echoamoy.holdlens.server.domain.processing.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingCallbackEntity {

    private Long id;
    private String serverTaskId;
    private String idempotencyKey;
    private String callbackStatus;
    private String processStatus;
    private String errorSummary;
    private Date createTime;
    private Date updateTime;

}
