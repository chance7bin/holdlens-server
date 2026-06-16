package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentWarningPO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String warningType;
    private String sourceType;
    private String sourceRefId;
    private Long snapshotId;
    private String fundCode;
    private String code;
    private String message;
    private String sourceSection;
    private Integer rowNumber;
    private String severity;
    private Date createTime;
    private Date updateTime;

}
