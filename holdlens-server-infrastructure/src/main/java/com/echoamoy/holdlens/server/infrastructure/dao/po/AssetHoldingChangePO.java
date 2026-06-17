package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetHoldingChangePO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 变更记录ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 持仓ID */
    private Long holdingId;

    /** 账户ID */
    private Long accountId;

    /** 资产ID */
    private Long assetId;

    /** 变更类型：create/update/delete/import/ocr/agent */
    private String changeType;

    /** 变更前金额 */
    private BigDecimal beforeAmount;

    /** 变更后金额 */
    private BigDecimal afterAmount;

    /** 币种 */
    private String currency;

    /** 变更原因 */
    private String changeReason;

    /** 来源类型：manual/file_import/ocr/agent/api_sync */
    private String sourceType;

    /** 来源引用ID */
    private String sourceRefId;

    /** 操作人ID */
    private Long operatorId;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
