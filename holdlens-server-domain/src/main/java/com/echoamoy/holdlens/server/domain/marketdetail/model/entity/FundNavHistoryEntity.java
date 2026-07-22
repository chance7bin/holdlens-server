package com.echoamoy.holdlens.server.domain.marketdetail.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FundNavHistoryEntity {
    private Long id;
    private String fundCode;
    private LocalDate navDate;
    private BigDecimal unitNav;
    private BigDecimal accumulatedNav;
    private BigDecimal dailyGrowthRate;
    private LocalDateTime sourceAsOf;
    private LocalDateTime fetchedAt;
}
