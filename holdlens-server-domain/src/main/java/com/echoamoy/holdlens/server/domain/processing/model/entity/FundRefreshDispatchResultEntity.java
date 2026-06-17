package com.echoamoy.holdlens.server.domain.processing.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundRefreshDispatchResultEntity {

    private boolean accepted;
    private String agentTaskRef;
    private String agentStatus;
    private String errorSummary;

}
