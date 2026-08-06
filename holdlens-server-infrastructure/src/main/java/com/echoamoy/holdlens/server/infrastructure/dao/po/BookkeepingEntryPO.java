package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookkeepingEntryPO {

    private Long id;
    private Long userId;
    private String requestId;
    private String type;
    private String categoryCode;
    private BigDecimal amount;
    private String currency;
    private LocalDate entryDate;
    private String note;
    private String status;
    private Date createTime;
    private Date updateTime;
}
