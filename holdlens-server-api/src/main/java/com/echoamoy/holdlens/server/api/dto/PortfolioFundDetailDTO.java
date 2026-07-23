package com.echoamoy.holdlens.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioFundDetailDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private List<HoldingDetail> holdings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldingDetail implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long recordId;
        private String assetRef;
        private String assetCode;
        private String assetName;
        private String assetKind;
        private String assetType;
        private BigDecimal amount;
        private String currency;
        private String status;
        private FundDetailDTO fundDetail;
    }
}
