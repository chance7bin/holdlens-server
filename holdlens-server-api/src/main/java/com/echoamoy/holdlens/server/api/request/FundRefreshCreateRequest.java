package com.echoamoy.holdlens.server.api.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundRefreshCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty
    private List<String> fundCodes;

    private String sourceType;

    private String sourceRefId;

}
