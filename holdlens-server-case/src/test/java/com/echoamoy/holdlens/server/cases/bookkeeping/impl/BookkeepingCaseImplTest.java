package com.echoamoy.holdlens.server.cases.bookkeeping.impl;

import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingCommand;
import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingResult;
import com.echoamoy.holdlens.server.domain.bookkeeping.adapter.repository.IBookkeepingRepository;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import com.echoamoy.holdlens.server.types.exception.AppException;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

public class BookkeepingCaseImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Test
    public void repeatedRequestReturnsFirstEntryAndDoesNotDuplicate() throws Exception {
        Fixture fixture = fixture();
        BookkeepingCommand.Create command = create(1L, "same", "EXPENSE", "FOOD", "1.00", LocalDate.now(ZONE));
        BookkeepingEntryEntity first = fixture.caseService.create(command);
        command.setAmount(new BigDecimal("2.00"));

        assertEquals(first.getId(), fixture.caseService.create(command).getId());
        assertEquals(1, fixture.repository.entries.size());
    }

    @Test
    public void listAndDetailAreUserIsolatedAndDeletedEntriesAreHidden() throws Exception {
        Fixture fixture = fixture();
        BookkeepingEntryEntity owned = fixture.caseService.create(create(1L, "one", "EXPENSE", "FOOD", "20.00", LocalDate.now(ZONE)));
        fixture.caseService.create(create(2L, "two", "INCOME", "SALARY", "100.00", LocalDate.now(ZONE)));

        assertEquals(1, fixture.caseService.queryEntries(1L, LocalDate.now(ZONE), LocalDate.now(ZONE), null, null).getEntries().size());
        expectInvisibleEntry(() -> fixture.caseService.queryEntry(2L, owned.getId()));
        fixture.caseService.delete(1L, owned.getId());
        expectInvisibleEntry(() -> fixture.caseService.queryEntry(1L, owned.getId()));
        assertTrue(fixture.caseService.queryEntries(1L, LocalDate.now(ZONE), LocalDate.now(ZONE), null, null).getEntries().isEmpty());
    }

    @Test
    public void statisticsFillsLeapMonthAndUsesCurrentAndHistoricalDivisors() throws Exception {
        Fixture fixture = fixture();
        fixture.caseService.create(create(1L, "leap", "EXPENSE", "FOOD", "29.00", LocalDate.of(2024, 2, 29)));
        BookkeepingResult.Statistics leap = fixture.caseService.statistics(1L, "EXPENSE", "MONTH", LocalDate.of(2024, 2, 10));
        assertEquals(29, leap.getPoints().size());
        assertEquals(new BigDecimal("1.00"), leap.getAverage());

        LocalDate today = LocalDate.now(ZONE);
        fixture.caseService.create(create(1L, "today", "EXPENSE", "FOOD", "10.00", today));
        BookkeepingResult.Statistics current = fixture.caseService.statistics(1L, "EXPENSE", "MONTH", today);
        assertEquals(new BigDecimal("10.00").divide(BigDecimal.valueOf(today.getDayOfMonth()), 2, java.math.RoundingMode.HALF_UP), current.getAverage());
        expectIllegalArgument(() -> fixture.caseService.statistics(1L, "EXPENSE", null, today));
        expectIllegalArgument(() -> fixture.caseService.statistics(1L, "EXPENSE", "MONTH", today.plusMonths(1)));
    }

    @Test
    public void monthlyAndYearlyBillsIncludeOnlyActiveDataAndEmptyDataIsZero() throws Exception {
        Fixture fixture = fixture();
        BookkeepingResult.YearlyBill empty = fixture.caseService.yearlyBill(1L);
        assertEquals(BigDecimal.ZERO.setScale(2), empty.getIncome());
        assertTrue(empty.getYears().isEmpty());

        BookkeepingEntryEntity income = fixture.caseService.create(create(1L, "income", "INCOME", "SALARY", "200.00", LocalDate.of(2024, 1, 1)));
        fixture.caseService.create(create(1L, "expense", "EXPENSE", "FOOD", "50.00", LocalDate.of(2025, 12, 31)));
        fixture.caseService.delete(1L, income.getId());
        BookkeepingResult.MonthlyBill monthly = fixture.caseService.monthlyBill(1L, 2025);
        assertEquals(12, monthly.getMonths().size());
        assertEquals(new BigDecimal("50.00"), monthly.getExpense());
        BookkeepingResult.YearlyBill yearly = fixture.caseService.yearlyBill(1L);
        assertEquals(1, yearly.getYears().size());
        assertEquals(Integer.valueOf(2025), yearly.getYears().get(0).getYear());
    }

    private Fixture fixture() throws Exception {
        BookkeepingCaseImpl caseService = new BookkeepingCaseImpl();
        FakeRepository repository = new FakeRepository();
        Field field = BookkeepingCaseImpl.class.getDeclaredField("bookkeepingRepository");
        field.setAccessible(true);
        field.set(caseService, repository);
        return new Fixture(caseService, repository);
    }

    private BookkeepingCommand.Create create(Long userId, String requestId, String type, String category, String amount, LocalDate date) {
        return BookkeepingCommand.Create.builder().userId(userId).requestId(requestId).type(type)
                .categoryCode(category).amount(new BigDecimal(amount)).entryDate(date).build();
    }

    private void expectIllegalArgument(Runnable action) {
        try { action.run(); fail("应抛出非法参数异常"); } catch (IllegalArgumentException ignored) { }
    }

    private void expectInvisibleEntry(Runnable action) {
        try {
            action.run();
            fail("应隐藏收支条目的归属和状态");
        } catch (AppException exception) {
            assertEquals("收支条目不存在或不可见", exception.getInfo());
        }
    }

    private record Fixture(BookkeepingCaseImpl caseService, FakeRepository repository) { }

    private static class FakeRepository implements IBookkeepingRepository {
        private final List<BookkeepingEntryEntity> entries = new ArrayList<>();
        public BookkeepingEntryEntity queryByUserAndRequestId(Long userId, String requestId) { return entries.stream().filter(e -> e.getUserId().equals(userId) && e.getRequestId().equals(requestId)).findFirst().orElse(null); }
        public BookkeepingEntryEntity queryActiveByUserAndId(Long userId, Long id) { return entries.stream().filter(e -> e.getUserId().equals(userId) && e.getId().equals(id) && e.getStatus().name().equals("ACTIVE")).findFirst().orElse(null); }
        public void insert(BookkeepingEntryEntity entry) { entry.setId((long) entries.size() + 1); entries.add(entry); }
        public void update(BookkeepingEntryEntity entry) { }
        public List<BookkeepingEntryEntity> queryActiveEntries(Long userId, LocalDate start, LocalDate end, BookkeepingEntryTypeEnumVO type, String categoryCode) {
            return entries.stream().filter(e -> e.getUserId().equals(userId) && e.getStatus().name().equals("ACTIVE") && !e.getEntryDate().isBefore(start) && !e.getEntryDate().isAfter(end) && (type == null || e.getType() == type) && (categoryCode == null || categoryCode.equals(e.getCategoryCode()))).sorted(Comparator.comparing(BookkeepingEntryEntity::getEntryDate).reversed().thenComparing(BookkeepingEntryEntity::getId, Comparator.reverseOrder())).toList();
        }
        public List<Integer> queryActiveYears(Long userId) { return entries.stream().filter(e -> e.getUserId().equals(userId) && e.getStatus().name().equals("ACTIVE")).map(e -> e.getEntryDate().getYear()).distinct().sorted(Comparator.reverseOrder()).toList(); }
    }
}
