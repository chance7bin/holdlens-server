package com.echoamoy.holdlens.server.domain.portfolio.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRecordChangeEntity {

    public static final String CREATE = "CREATE";
    public static final String UPDATE_AMOUNT = "UPDATE_AMOUNT";
    public static final String SPLIT_OUT = "SPLIT_OUT";
    public static final String SPLIT_IN = "SPLIT_IN";
    public static final String ARCHIVE = "ARCHIVE";
    public static final String RESTORE = "RESTORE";
    public static final String DELETE = "DELETE";

    private Long id;
    private String operationId;
    private Long userId;
    private Long recordId;
    private String changeType;
    private BigDecimal beforeAmount;
    private BigDecimal afterAmount;
    private String currency;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private LocalDateTime createTime;
}
