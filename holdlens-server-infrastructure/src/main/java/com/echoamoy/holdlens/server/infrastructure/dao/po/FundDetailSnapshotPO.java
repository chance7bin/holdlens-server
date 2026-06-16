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
public class FundDetailSnapshotPO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String schemaVersion;
    private Date generatedAt;
    private String snapshotStatus;
    private String sourceType;
    private String sourceRefId;
    private String dataSourcesJson;
    private Date createTime;
    private Date updateTime;

}
