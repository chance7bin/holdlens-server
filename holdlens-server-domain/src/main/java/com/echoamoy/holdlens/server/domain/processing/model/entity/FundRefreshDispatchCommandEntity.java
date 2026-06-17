package com.echoamoy.holdlens.server.domain.processing.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundRefreshDispatchCommandEntity {

    private String schemaVersion;
    private String serverTaskId;
    private List<String> fundCodes;
    private Boolean allowNetwork;
    private String callbackUrl;

}
