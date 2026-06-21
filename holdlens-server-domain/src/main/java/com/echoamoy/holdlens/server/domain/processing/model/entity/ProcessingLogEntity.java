package com.echoamoy.holdlens.server.domain.processing.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingLogEntity {

    private String sourceRefId;
    private String module;
    private String event;
    private String message;
    private String severity;

}
