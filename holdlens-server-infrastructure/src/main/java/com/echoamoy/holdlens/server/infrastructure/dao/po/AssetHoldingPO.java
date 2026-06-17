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
public class AssetHoldingPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 持仓ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 账户ID */
    private Long accountId;

    /** 资产ID */
    private Long assetId;

    /** 资产分类 */
    private String assetCategory;

    /** 持仓来源账户类型：fund_account/stock_account/unknown */
    private String holdingSource;

    /** 持仓金额 */
    private BigDecimal amount;

    /** 币种 */
    private String currency;

    /** 原始展示金额 */
    private String amountDisplay;

    /** 金额缺失原因 */
    private String amountMissingReason;

    /** 字段级缺失原因JSON */
    private String missingReasonsJson;

    /** 状态：active/closed/deleted */
    private String status;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
