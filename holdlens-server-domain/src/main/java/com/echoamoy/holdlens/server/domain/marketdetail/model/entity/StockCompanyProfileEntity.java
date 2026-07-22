package com.echoamoy.holdlens.server.domain.marketdetail.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StockCompanyProfileEntity {
    private Long id;
    private String stockCode;
    private String market;
    private String companyName;
    private String industry;
    private String businessSummary;
    private String companyProfile;
    private String website;
    private LocalDateTime sourceAsOf;
    private LocalDateTime fetchedAt;
}
