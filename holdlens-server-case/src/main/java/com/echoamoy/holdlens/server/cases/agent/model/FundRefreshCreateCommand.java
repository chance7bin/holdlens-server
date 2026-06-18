package com.echoamoy.holdlens.server.cases.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundRefreshCreateCommand {

    private List<String> fundCodes;

}
