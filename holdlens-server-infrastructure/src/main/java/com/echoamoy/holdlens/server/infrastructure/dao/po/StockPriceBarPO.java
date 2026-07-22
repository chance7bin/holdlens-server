package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StockPriceBarPO {
    private Long id;
    private String stockCode;
    private String market;
    private String granularity;
    private Date barTime;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private String currency;
    private Date sourceAsOf;
    private Date fetchedAt;
    private Date createTime;
    private Date updateTime;
}
