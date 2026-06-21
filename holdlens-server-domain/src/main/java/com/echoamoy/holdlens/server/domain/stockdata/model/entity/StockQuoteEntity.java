package com.echoamoy.holdlens.server.domain.stockdata.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuoteEntity {

    private String stockCode;
    private String market;
    private String stockName;
    private Date tradeDate;
    private BigDecimal dailyReturn;
    private Date quoteTime;

}
