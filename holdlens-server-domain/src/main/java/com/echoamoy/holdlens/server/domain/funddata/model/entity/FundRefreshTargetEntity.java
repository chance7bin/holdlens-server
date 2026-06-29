package com.echoamoy.holdlens.server.domain.funddata.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundRefreshTargetEntity {

    private Long id;

    private String fundCode;

    private String fundName;

}
