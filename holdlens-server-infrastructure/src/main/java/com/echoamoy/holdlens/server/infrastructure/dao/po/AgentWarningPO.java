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

    /** 警告ID */
    private Long id;

    /** 警告类型：parse/refresh/ocr/import/agent */
    private String warningType;

    /** 来源类型：manual/file_import/ocr/agent/api_sync */
    private String sourceType;

    /** 来源引用ID */
    private String sourceRefId;

    /** 基金快照ID */
    private Long snapshotId;

    /** 基金代码 */
    private String fundCode;

    /** 警告代码 */
    private String code;

    /** 警告消息 */
    private String message;

    /** 来源章节 */
    private String sourceSection;

    /** 来源行号 */
    private Integer sourceRowNumber;

    /** 级别：info/warning/error */
    private String severity;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
