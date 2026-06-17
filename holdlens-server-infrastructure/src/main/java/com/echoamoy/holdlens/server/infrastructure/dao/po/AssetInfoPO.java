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
public class AssetInfoPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 资产代码 */
    private String assetCode;

    /** 资产名称 */
    private String assetName;

    /** 资产大类：fund/stock/cash/unknown */
    private String assetKind;

    /** 资产类型：ETF/LOF/开放式基金/普通股票等 */
    private String assetType;

    /** 市场标识：SH/SZ/HK/US等 */
    private String market;

    /** 状态：enabled/disabled/deleted */
    private String status;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
