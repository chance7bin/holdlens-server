package com.echoamoy.holdlens.server.cases.bookkeeping.impl;

import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingCommand;
import com.echoamoy.holdlens.server.domain.bookkeeping.adapter.repository.IBookkeepingCategoryRepository;
import com.echoamoy.holdlens.server.domain.bookkeeping.adapter.repository.IBookkeepingRepository;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingCategoryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryStatusEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class BookkeepingCategoryCaseImplTest {

    @Test
    public void createsIdempotentlyAndRejectsInvalidNameIconAndDuplicate() throws Exception {
        Fixture fixture = new Fixture();
        BookkeepingCommand.CreateCategory command = createCategory("request-1", " 自定义 ", "food");

        BookkeepingCategoryEntity first = fixture.service.createCategory(command);

        assertEquals(first.getCode(), fixture.service.createCategory(command).getCode());
        assertEquals("自定义", first.getName());
        assertEquals("USER", first.getScope());
        assertTrue(first.isEnabled());
        assertThrows(IllegalArgumentException.class, () -> fixture.service.createCategory(
                createCategory("request-2", "五个可见字", "food")
        ));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.createCategory(
                createCategory("request-3", "新类", "not-an-icon")
        ));
        assertThrows(IllegalArgumentException.class, () -> fixture.service.createCategory(
                createCategory("request-4", "自定义", "food")
        ));
    }

    @Test
    public void concurrentSameRequestReturnsFirstCategoryWithoutAddingASecondConfig() throws Exception {
        Fixture fixture = new Fixture();
        fixture.categories.concurrentWinner = BookkeepingCategoryEntity.builder()
                .id(88L)
                .code("CUS_WINNER")
                .scope("USER")
                .ownerUserId(1L)
                .type(BookkeepingEntryTypeEnumVO.EXPENSE)
                .name("先到")
                .iconKey("shopping")
                .defaultEnabled(true)
                .defaultSortOrder(10)
                .createRequestId("same-request")
                .build();

        BookkeepingCategoryEntity result = fixture.service.createCategory(
                createCategory("same-request", "后到", "food")
        );

        assertEquals("CUS_WINNER", result.getCode());
        assertEquals("先到", result.getName());
        assertEquals(0, fixture.categories.upsertCount);
    }

    @Test
    public void validatesCompleteOrderAndDisablesWithoutRestoringEntries() throws Exception {
        Fixture fixture = new Fixture();
        fixture.categories.values.add(systemCategory(1L, "FOOD", "餐饮", 10));
        fixture.categories.values.add(systemCategory(2L, "SHOPPING", "购物", 20));
        fixture.categories.values.add(BookkeepingCategoryEntity.builder()
                .id(3L)
                .code("CUS_PRIVATE")
                .scope("USER")
                .ownerUserId(1L)
                .type(BookkeepingEntryTypeEnumVO.EXPENSE)
                .name("私有")
                .iconKey("food")
                .status("ENABLED")
                .sortOrder(30)
                .build());

        assertThrows(IllegalArgumentException.class, () -> fixture.service.reorderCategories(
                1L,
                "EXPENSE",
                List.of("FOOD")
        ));
        fixture.service.reorderCategories(
                1L,
                "EXPENSE",
                List.of("SHOPPING", "FOOD", "CUS_PRIVATE")
        );
        assertEquals(Integer.valueOf(10), fixture.categories.byCode("SHOPPING").getSortOrder());

        fixture.entries.values.add(BookkeepingEntryEntity.builder()
                .id(1L)
                .userId(1L)
                .type(BookkeepingEntryTypeEnumVO.EXPENSE)
                .categoryCode("FOOD")
                .status(BookkeepingEntryStatusEnumVO.ACTIVE)
                .entryDate(LocalDate.now())
                .build());
        assertEquals(1, fixture.service.disableCategory(1L, "FOOD"));
        assertEquals(BookkeepingEntryStatusEnumVO.DELETED, fixture.entries.values.get(0).getStatus());
        fixture.service.enableCategory(1L, "FOOD");
        assertEquals(BookkeepingEntryStatusEnumVO.DELETED, fixture.entries.values.get(0).getStatus());
        assertThrows(IllegalArgumentException.class, () -> fixture.service.disableCategory(2L, "CUS_PRIVATE"));
    }

    @Test
    public void listAndStatisticsResolveCategoryMetadataWithoutPerCategoryQueries() throws Exception {
        Fixture fixture = new Fixture();
        fixture.categories.values.add(systemCategory(1L, "FOOD", "餐饮", 10));
        fixture.categories.values.add(BookkeepingCategoryEntity.builder()
                .id(2L)
                .code("SALARY")
                .scope("SYSTEM")
                .type(BookkeepingEntryTypeEnumVO.INCOME)
                .name("工资")
                .iconKey("salary")
                .status("ENABLED")
                .sortOrder(10)
                .build());
        fixture.entries.values.add(entry(1L, BookkeepingEntryTypeEnumVO.EXPENSE, "FOOD"));
        fixture.entries.values.add(entry(2L, BookkeepingEntryTypeEnumVO.INCOME, "SALARY"));

        fixture.categories.visibleCalls.clear();
        List<BookkeepingEntryEntity> entries = fixture.service.queryEntries(
                1L,
                LocalDate.now(),
                LocalDate.now(),
                null,
                null
        ).getEntries();
        assertEquals(2, entries.size());
        assertEquals(Integer.valueOf(1), fixture.categories.visibleCalls.get(BookkeepingEntryTypeEnumVO.EXPENSE));
        assertEquals(Integer.valueOf(1), fixture.categories.visibleCalls.get(BookkeepingEntryTypeEnumVO.INCOME));

        fixture.categories.visibleCalls.clear();
        fixture.service.statistics(1L, "EXPENSE", "WEEK", LocalDate.now());
        assertEquals(Integer.valueOf(1), fixture.categories.visibleCalls.get(BookkeepingEntryTypeEnumVO.EXPENSE));
    }

    @Test
    public void mutatingCategoryOperationsDeclareTransactionBoundaries() throws Exception {
        assertTransactional("createCategory", BookkeepingCommand.CreateCategory.class);
        assertTransactional("enableCategory", Long.class, String.class);
        assertTransactional("disableCategory", Long.class, String.class);
        assertTransactional("reorderCategories", Long.class, String.class, List.class);
    }

    private void assertTransactional(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = BookkeepingCaseImpl.class.getMethod(methodName, parameterTypes);
        assertNotNull(method.getAnnotation(Transactional.class));
    }

    private BookkeepingCommand.CreateCategory createCategory(
            String requestId,
            String name,
            String iconKey
    ) {
        return BookkeepingCommand.CreateCategory.builder()
                .userId(1L)
                .requestId(requestId)
                .type("EXPENSE")
                .name(name)
                .iconKey(iconKey)
                .build();
    }

    private static BookkeepingCategoryEntity systemCategory(
            Long id,
            String code,
            String name,
            int sortOrder
    ) {
        return BookkeepingCategoryEntity.builder()
                .id(id)
                .code(code)
                .scope("SYSTEM")
                .type(BookkeepingEntryTypeEnumVO.EXPENSE)
                .name(name)
                .iconKey("food")
                .status("ENABLED")
                .sortOrder(sortOrder)
                .build();
    }

    private static BookkeepingEntryEntity entry(
            Long id,
            BookkeepingEntryTypeEnumVO type,
            String categoryCode
    ) {
        return BookkeepingEntryEntity.builder()
                .id(id)
                .userId(1L)
                .requestId("entry-" + id)
                .type(type)
                .categoryCode(categoryCode)
                .amount(java.math.BigDecimal.ONE)
                .currency("CNY")
                .entryDate(LocalDate.now())
                .status(BookkeepingEntryStatusEnumVO.ACTIVE)
                .build();
    }

    private static class Fixture {
        private final BookkeepingCaseImpl service = new BookkeepingCaseImpl();
        private final Entries entries = new Entries();
        private final Categories categories = new Categories(entries);

        private Fixture() throws Exception {
            set("categoryRepository", categories);
            set("bookkeepingRepository", entries);
        }

        private void set(String name, Object value) throws Exception {
            Field field = BookkeepingCaseImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(service, value);
        }
    }

    private static class Categories implements IBookkeepingCategoryRepository {
        private final List<BookkeepingCategoryEntity> values = new ArrayList<>();
        private final Map<BookkeepingEntryTypeEnumVO, Integer> visibleCalls = new HashMap<>();
        private final Entries entries;
        private BookkeepingCategoryEntity concurrentWinner;
        private int upsertCount;

        private Categories(Entries entries) {
            this.entries = entries;
        }

        @Override
        public List<BookkeepingCategoryEntity> queryVisible(
                Long userId,
                BookkeepingEntryTypeEnumVO type
        ) {
            visibleCalls.merge(type, 1, Integer::sum);
            return values.stream()
                    .filter(category -> category.getType() == type)
                    .filter(category -> "SYSTEM".equals(category.getScope())
                            || Objects.equals(category.getOwnerUserId(), userId))
                    .toList();
        }

        @Override
        public BookkeepingCategoryEntity queryVisibleByCode(Long userId, String code) {
            return values.stream()
                    .filter(category -> category.getCode().equals(code))
                    .filter(category -> "SYSTEM".equals(category.getScope())
                            || Objects.equals(category.getOwnerUserId(), userId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public BookkeepingCategoryEntity queryByOwnerAndRequestId(Long userId, String requestId) {
            return values.stream()
                    .filter(category -> Objects.equals(category.getOwnerUserId(), userId))
                    .filter(category -> requestId.equals(category.getCreateRequestId()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean insertUserCategory(BookkeepingCategoryEntity category) {
            if (concurrentWinner != null) {
                copy(concurrentWinner, category);
                concurrentWinner = null;
                return false;
            }
            category.setId((long) values.size() + 10);
            values.add(category);
            return true;
        }

        @Override
        public void upsertConfig(Long userId, Long categoryId, String status, Integer sortOrder) {
            upsertCount++;
            BookkeepingCategoryEntity category = values.stream()
                    .filter(value -> value.getId().equals(categoryId))
                    .findFirst()
                    .orElseThrow();
            category.setStatus(status);
            category.setSortOrder(sortOrder);
        }

        @Override
        public int disableAndDeleteActiveEntries(Long userId, String type, String categoryCode) {
            return entries.softDelete(userId, type, categoryCode);
        }

        private BookkeepingCategoryEntity byCode(String code) {
            return values.stream()
                    .filter(category -> category.getCode().equals(code))
                    .findFirst()
                    .orElse(null);
        }

        private void copy(BookkeepingCategoryEntity source, BookkeepingCategoryEntity target) {
            target.setId(source.getId());
            target.setCode(source.getCode());
            target.setScope(source.getScope());
            target.setOwnerUserId(source.getOwnerUserId());
            target.setType(source.getType());
            target.setName(source.getName());
            target.setIconKey(source.getIconKey());
            target.setDefaultEnabled(source.getDefaultEnabled());
            target.setDefaultSortOrder(source.getDefaultSortOrder());
            target.setCreateRequestId(source.getCreateRequestId());
        }
    }

    private static class Entries implements IBookkeepingRepository {
        private final List<BookkeepingEntryEntity> values = new ArrayList<>();

        @Override
        public BookkeepingEntryEntity queryByUserAndRequestId(Long userId, String requestId) {
            return null;
        }

        @Override
        public BookkeepingEntryEntity queryActiveByUserAndId(Long userId, Long entryId) {
            return null;
        }

        @Override
        public void insert(BookkeepingEntryEntity entry) {
        }

        @Override
        public void update(BookkeepingEntryEntity entry) {
        }

        @Override
        public List<BookkeepingEntryEntity> queryActiveEntries(
                Long userId,
                LocalDate startDate,
                LocalDate endDate,
                BookkeepingEntryTypeEnumVO type,
                String categoryCode
        ) {
            return values.stream()
                    .filter(entry -> Objects.equals(entry.getUserId(), userId))
                    .filter(entry -> entry.getStatus() == BookkeepingEntryStatusEnumVO.ACTIVE)
                    .filter(entry -> type == null || entry.getType() == type)
                    .filter(entry -> categoryCode == null || entry.getCategoryCode().equals(categoryCode))
                    .toList();
        }

        @Override
        public List<Integer> queryActiveYears(Long userId) {
            return List.of();
        }

        private int softDelete(Long userId, String type, String categoryCode) {
            int deleted = 0;
            for (BookkeepingEntryEntity entry : values) {
                if (Objects.equals(entry.getUserId(), userId)
                        && entry.getType().name().equals(type)
                        && entry.getCategoryCode().equals(categoryCode)
                        && entry.getStatus() == BookkeepingEntryStatusEnumVO.ACTIVE) {
                    entry.setStatus(BookkeepingEntryStatusEnumVO.DELETED);
                    deleted++;
                }
            }
            return deleted;
        }
    }
}
