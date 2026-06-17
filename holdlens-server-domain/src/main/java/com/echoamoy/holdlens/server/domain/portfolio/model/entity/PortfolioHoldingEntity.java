package com.echoamoy.holdlens.server.domain.portfolio.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioHoldingEntity {

    private Long userId;
    private Long holdingId;
    private Long accountId;
    private String accountName;
    private String accountType;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String assetKind;
    private String assetType;
    private String assetCategory;
    private String holdingSource;
    private BigDecimal amount;
    private String currency;
    private String amountDisplay;
    private String amountMissingReason;
    private String missingReasonsJson;
    private String status;

    public String fundCodeOrNull() {
        if (assetCode == null || assetCode.isBlank()) {
            return null;
        }
        return assetCode.trim();
    }

}
