package com.echoamoy.holdlens.server.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMarketCurrentPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 股票行情ID */
    private Long id;

    /** 股票代码 */
    private String stockCode;

    /** 市场标识 */
    private String market;

    /** 股票简称 */
    private String stockName;

    /** 交易日期 */
    private Date tradeDate;

    /** 当日涨跌幅 */
    private BigDecimal dailyReturn;

    /** 行情时间 */
    private LocalDateTime quoteTime;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

}
