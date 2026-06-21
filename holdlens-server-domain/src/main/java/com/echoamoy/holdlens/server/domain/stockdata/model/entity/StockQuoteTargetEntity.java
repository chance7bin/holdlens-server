package com.echoamoy.holdlens.server.domain.stockdata.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuoteTargetEntity {

    private String stockCode;
    private String market;

}
