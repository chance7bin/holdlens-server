package com.echoamoy.holdlens.server.domain.marketdetail.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StockPriceBarEntity {
    private Long id;
    private String stockCode;
    private String market;
    private String granularity;
    private LocalDateTime barTime;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private String currency;
    private LocalDateTime sourceAsOf;
    private LocalDateTime fetchedAt;
}
