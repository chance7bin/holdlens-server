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
public class AssetAccountPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账户ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 账户名称 */
    private String accountName;

    /** 账户类型：fund/stock/unknown */
    private String accountType;

    /** 状态：enabled/disabled/deleted */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
