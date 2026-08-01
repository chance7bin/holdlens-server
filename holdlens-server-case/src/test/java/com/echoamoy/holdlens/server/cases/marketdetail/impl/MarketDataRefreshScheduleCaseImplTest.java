package com.echoamoy.holdlens.server.cases.marketdetail.impl;

import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
import com.echoamoy.holdlens.server.cases.marketdetail.IMarketDetailCase;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class MarketDataRefreshScheduleCaseImplTest {

    @Test
    public void aShareRefreshRunsOnlyThirtyMinutesAfterCloseAndSkipsClosedDays() throws Exception {
        List<String> calls = new ArrayList<>();
        MarketDataRefreshScheduleCaseImpl schedule = newSchedule(calls);

        set(schedule, "clock", fixed("2026-07-31T01:35:00Z"));
        Assert.assertFalse(schedule.runAShareMarketRefresh());
        Assert.assertTrue(calls.isEmpty());

        set(schedule, "clock", fixed("2026-07-31T07:30:00Z"));
        Assert.assertTrue(schedule.runAShareMarketRefresh());
        Assert.assertEquals(List.of("createAndDispatchAShareMarket"), calls);

        calls.clear();
        set(schedule, "aShareClosedDates", "2026-07-31");
        Assert.assertFalse(schedule.runAShareMarketRefresh());
        Assert.assertTrue(calls.isEmpty());
    }

    @Test
    public void usMarketRefreshesFifteenMinutesAfterRegularOrEarlyClose() throws Exception {
        List<String> calls = new ArrayList<>();
        MarketDataRefreshScheduleCaseImpl schedule = newSchedule(calls);
        set(schedule, "usStockEarlyCloses", "2026-07-02=13:00");
        set(schedule, "clock", fixed("2026-07-02T17:15:00Z"));

        Assert.assertTrue(schedule.runUSStockMarketRefresh());
        Assert.assertEquals(List.of("createAndDispatchUSStockMarket"), calls);

        calls.clear();
        set(schedule, "clock", fixed("2026-07-02T17:35:00Z"));
        Assert.assertFalse(schedule.runUSStockMarketRefresh());
        Assert.assertTrue(calls.isEmpty());

        set(schedule, "usStockEarlyCloses", "");
        set(schedule, "clock", fixed("2026-07-02T20:15:00Z"));
        Assert.assertTrue(schedule.runUSStockMarketRefresh());
    }

    @Test
    public void activeDetailSchedulesDelegateWithoutWritingPerUserState() throws Exception {
        List<String> calls = new ArrayList<>();
        MarketDataRefreshScheduleCaseImpl schedule = newSchedule(calls);
        set(schedule, "clock", fixed("2026-07-31T02:00:00Z"));

        Assert.assertEquals(7, schedule.runFundDetailRefresh());
        Assert.assertEquals(7, schedule.runStockDetailRefresh("A_SHARE"));
        Assert.assertTrue(calls.contains("scheduleActiveFundDetails"));
        Assert.assertTrue(calls.contains("scheduleActiveStockDetails"));
    }

    private MarketDataRefreshScheduleCaseImpl newSchedule(List<String> calls) throws Exception {
        MarketDataRefreshScheduleCaseImpl schedule = new MarketDataRefreshScheduleCaseImpl();
        IAgentFundRefreshCase agent = (IAgentFundRefreshCase) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{IAgentFundRefreshCase.class}, (proxy, method, args) -> {
                    calls.add(method.getName());
                    return null;
                });
        IMarketDetailCase marketDetail = (IMarketDetailCase) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{IMarketDetailCase.class}, (proxy, method, args) -> {
                    calls.add(method.getName());
                    if (method.getReturnType() == int.class) return 7;
                    return null;
                });
        set(schedule, "agentFundRefreshCase", agent);
        set(schedule, "marketDetailCase", marketDetail);
        set(schedule, "aShareClosedDates", "");
        set(schedule, "usStockClosedDates", "");
        set(schedule, "usStockEarlyCloses", "");
        return schedule;
    }

    private Clock fixed(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    private void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
