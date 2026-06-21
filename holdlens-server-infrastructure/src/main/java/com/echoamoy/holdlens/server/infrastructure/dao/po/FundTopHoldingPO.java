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
public class FundTopHoldingPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 基金重仓ID */
    private Long id;

    /** 基金代码 */
    private String fundCode;

    /** 重仓排名 */
    private Integer rankNo;

    /** 股票简称 */
    private String stockName;

    /** 股票代码 */
    private String stockCode;

    /** 市场标识 */
    private String market;

    /** 持仓占比 */
    private BigDecimal holdingRatio;

    /** 较上季度变化类型：new/increased/decreased/unchanged/removed/not_applicable/unknown */
    private String quarterChangeType;

    /** 较上季度变化值 */
    private BigDecimal quarterChangeValue;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
