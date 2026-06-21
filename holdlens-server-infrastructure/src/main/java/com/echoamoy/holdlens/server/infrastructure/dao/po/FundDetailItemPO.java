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
public class FundDetailItemPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 基金详情ID */
    private Long id;

    /** 基金资产ID */
    private Long fundAssetId;

    /** 基金代码 */
    private String fundCode;

    /** 基金名称 */
    private String fundName;

    /** 申购状态：open/closed/limited/suspended/unknown */
    private String buyStatus;

    /** 单日申购限额展示文本 */
    private String dailyPurchaseLimit;

    /** 涨跌幅数据日期 */
    private Date returnsAsOf;

    /** 重仓披露日期 */
    private Date topHoldingsAsOf;

    /** 公开重仓状态：public/no_public_stock_holdings/missing */
    private String publicHoldingsStatus;

    /** 近1月涨跌幅 */
    private BigDecimal oneMonthReturn;

    /** 近3月涨跌幅 */
    private BigDecimal threeMonthsReturn;

    /** 近6月涨跌幅 */
    private BigDecimal sixMonthsReturn;

    /** 近1年涨跌幅 */
    private BigDecimal oneYearReturn;

    /** 近3年涨跌幅 */
    private BigDecimal threeYearsReturn;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
