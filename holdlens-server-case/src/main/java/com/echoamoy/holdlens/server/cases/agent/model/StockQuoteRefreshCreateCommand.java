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
public class StockQuoteRefreshCreateCommand {

    private List<Stock> stocks;

    private String trigger;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stock {

        private String stockCode;

        private String market;

    }

}
