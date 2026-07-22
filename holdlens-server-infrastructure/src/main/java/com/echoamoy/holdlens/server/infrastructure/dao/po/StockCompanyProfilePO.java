package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StockCompanyProfilePO {
    private Long id;
    private String stockCode;
    private String market;
    private String companyName;
    private String industry;
    private String businessSummary;
    private String companyProfile;
    private String website;
    private Date sourceAsOf;
    private Date fetchedAt;
    private Date createTime;
    private Date updateTime;
}
