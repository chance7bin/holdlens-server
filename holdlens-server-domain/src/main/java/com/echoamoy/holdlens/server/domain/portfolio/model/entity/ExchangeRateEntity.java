package com.echoamoy.holdlens.server.domain.portfolio.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateEntity {

    public static final String CNY = "CNY";

    private Long id;
    private String baseCurrency;
    private String quoteCurrency;
    private BigDecimal rate;
    private String source;
    private LocalDateTime sourceAsOf;
    private LocalDateTime fetchedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static ExchangeRateEntity current(String baseCurrency, String quoteCurrency, BigDecimal rate,
                                             String source, LocalDateTime sourceAsOf, LocalDateTime fetchedAt) {
        String base = normalizeCurrency(baseCurrency);
        String quote = normalizeCurrency(quoteCurrency);
        if (CNY.equals(base) || !CNY.equals(quote)) {
            throw new IllegalArgumentException("当前只允许保存外币兑人民币汇率");
        }
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("汇率必须大于零");
        }
        return ExchangeRateEntity.builder().baseCurrency(base).quoteCurrency(quote).rate(rate)
                .source(source == null || source.isBlank() ? null : source.trim())
                .sourceAsOf(sourceAsOf).fetchedAt(fetchedAt).build();
    }

    public static String normalizeCurrency(String value) {
        if (value == null || !value.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException("币种不合法");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
