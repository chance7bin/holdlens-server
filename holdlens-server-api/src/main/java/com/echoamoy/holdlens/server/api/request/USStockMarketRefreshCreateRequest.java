package com.echoamoy.holdlens.server.api.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class USStockMarketRefreshCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String trigger;

}
