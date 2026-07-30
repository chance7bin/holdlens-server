package com.echoamoy.holdlens.server.domain.marketdetail.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FundPeriodPerformanceEntity {
    private Long id;
    private String fundCode;
    private String period;
    private BigDecimal fundReturn;
    private BigDecimal peerAverage;
    private Integer peerRank;
    private Integer peerTotal;
    private Integer rankChange;
    private LocalDate asOf;
    private LocalDateTime fetchedAt;
}
