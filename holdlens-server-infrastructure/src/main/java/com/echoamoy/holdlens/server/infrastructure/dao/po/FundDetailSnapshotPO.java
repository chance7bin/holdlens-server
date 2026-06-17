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

    /** 基金详情快照ID */
    private Long id;

    /** agent契约版本 */
    private String schemaVersion;

    /** 快照生成时间 */
    private Date generatedAt;

    /** 快照状态：success/partial/failed */
    private String snapshotStatus;

    /** 来源类型：agent/api_sync */
    private String sourceType;

    /** 来源引用ID */
    private String sourceRefId;

    /** 数据来源元信息JSON */
    private String dataSourcesJson;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
