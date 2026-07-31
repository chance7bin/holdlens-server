package com.echoamoy.holdlens.server.cases.marketdetail.impl;

import com.echoamoy.holdlens.server.cases.agent.IAgentFundRefreshCase;
import com.echoamoy.holdlens.server.cases.agent.model.AShareMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.agent.model.USStockMarketRefreshCreateCommand;
import com.echoamoy.holdlens.server.cases.marketdetail.IMarketDataRefreshScheduleCase;
import com.echoamoy.holdlens.server.cases.marketdetail.IMarketDetailCase;
import com.echoamoy.holdlens.server.domain.stockdata.model.entity.StockMarketEntity;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MarketDataRefreshScheduleCaseImpl implements IMarketDataRefreshScheduleCase {

    private static final ZoneId A_SHARE_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneId US_STOCK_ZONE = ZoneId.of("America/New_York");
    private static final String TRIGGER = "schedule";

    @Resource private IAgentFundRefreshCase agentFundRefreshCase;
    @Resource private IMarketDetailCase marketDetailCase;
    private Clock clock = Clock.systemUTC();

    @Value("${holdlens.market-calendar.a-share.closed-dates:}") private String aShareClosedDates;
    @Value("${holdlens.market-calendar.us-stock.closed-dates:}") private String usStockClosedDates;
    @Value("${holdlens.market-calendar.us-stock.early-closes:}") private String usStockEarlyCloses;

    @Override
    public boolean runAShareMarketRefresh() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(A_SHARE_ZONE);
        if (!isTradingDay(now.toLocalDate(), parseDates(aShareClosedDates)) || !isAShareRefreshPoint(now.toLocalTime())) {
            return false;
        }
        try {
            agentFundRefreshCase.createAndDispatchAShareMarket(
                    AShareMarketRefreshCreateCommand.builder().trigger(TRIGGER).build());
            return true;
        } catch (RuntimeException exception) {
            log.info("A 股全市场行情定时刷新跳过 reason={}", exception.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean runUSStockMarketRefresh() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(US_STOCK_ZONE);
        Map<LocalDate, LocalTime> earlyCloses = parseEarlyCloses(usStockEarlyCloses);
        if (!isTradingDay(now.toLocalDate(), parseDates(usStockClosedDates))
                || !isSessionRefreshPoint(now.toLocalTime(), LocalTime.of(9, 30),
                earlyCloses.getOrDefault(now.toLocalDate(), LocalTime.of(16, 0)))) {
            return false;
        }
        try {
            agentFundRefreshCase.createAndDispatchUSStockMarket(
                    USStockMarketRefreshCreateCommand.builder().trigger(TRIGGER).build());
            return true;
        } catch (RuntimeException exception) {
            log.info("美股全市场行情定时刷新跳过 reason={}", exception.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public int runFundDetailRefresh() {
        return marketDetailCase.scheduleActiveFundDetails();
    }

    @Override
    public int runStockDetailRefresh(String market) {
        ZoneId zone = StockMarketEntity.MARKET_US_STOCK.equals(market) ? US_STOCK_ZONE : A_SHARE_ZONE;
        Set<LocalDate> closedDates = StockMarketEntity.MARKET_US_STOCK.equals(market)
                ? parseDates(usStockClosedDates) : parseDates(aShareClosedDates);
        if (!isTradingDay(ZonedDateTime.now(clock).withZoneSameInstant(zone).toLocalDate(), closedDates)) return 0;
        return marketDetailCase.scheduleActiveStockDetails(market);
    }

    private boolean isAShareRefreshPoint(LocalTime time) {
        return isIntervalRefreshPoint(time, LocalTime.of(9, 30), LocalTime.of(11, 30))
                || isSessionRefreshPoint(time, LocalTime.of(13, 0), LocalTime.of(15, 0));
    }

    private boolean isSessionRefreshPoint(LocalTime time, LocalTime open, LocalTime close) {
        LocalTime minute = time.truncatedTo(ChronoUnit.MINUTES);
        if (minute.equals(close.plusMinutes(5))) return true;
        return isIntervalRefreshPoint(minute, open, close);
    }

    private boolean isIntervalRefreshPoint(LocalTime time, LocalTime open, LocalTime close) {
        LocalTime minute = time.truncatedTo(ChronoUnit.MINUTES);
        LocalTime first = open.plusMinutes(5);
        if (minute.isBefore(first) || !minute.isBefore(close)) return false;
        return ChronoUnit.MINUTES.between(first, minute) % 30 == 0;
    }

    private boolean isTradingDay(LocalDate date, Set<LocalDate> closedDates) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !closedDates.contains(date);
    }

    private Set<LocalDate> parseDates(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(text -> !text.isEmpty())
                .map(LocalDate::parse).collect(Collectors.toUnmodifiableSet());
    }

    private Map<LocalDate, LocalTime> parseEarlyCloses(String value) {
        if (value == null || value.isBlank()) return Map.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(text -> !text.isEmpty())
                .map(text -> text.split("=", 2)).collect(Collectors.toUnmodifiableMap(
                        parts -> LocalDate.parse(parts[0]), parts -> LocalTime.parse(parts[1])));
    }
}
