package com.echoamoy.holdlens.server.domain.portfolio.model.entity;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class ExchangeRateEntityTest {

    @Test
    public void acceptsForeignCurrencyToCny() {
        ExchangeRateEntity rate = ExchangeRateEntity.current("usd", "cny", new BigDecimal("7.2"),
                "manual", null, null);

        Assert.assertEquals("USD", rate.getBaseCurrency());
        Assert.assertEquals("CNY", rate.getQuoteCurrency());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCnyAsBaseCurrency() {
        ExchangeRateEntity.current("CNY", "USD", new BigDecimal("0.14"), null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsForeignCrossRate() {
        ExchangeRateEntity.current("USD", "HKD", new BigDecimal("7.8"), null, null, null);
    }
}
