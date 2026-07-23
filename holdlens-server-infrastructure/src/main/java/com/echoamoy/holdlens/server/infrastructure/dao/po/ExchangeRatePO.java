package com.echoamoy.holdlens.server.infrastructure.dao.po;

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
public class ExchangeRatePO {
    private Long id;
    private String baseCurrency;
    private String quoteCurrency;
    private BigDecimal rate;
    private String source;
    private Date sourceAsOf;
    private Date fetchedAt;
    private Date createTime;
    private Date updateTime;
}
