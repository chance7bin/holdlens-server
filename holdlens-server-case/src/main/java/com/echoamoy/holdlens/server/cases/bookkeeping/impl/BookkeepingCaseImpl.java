package com.echoamoy.holdlens.server.cases.bookkeeping.impl;

import com.echoamoy.holdlens.server.cases.bookkeeping.IBookkeepingCase;
import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingCommand;
import com.echoamoy.holdlens.server.cases.bookkeeping.model.BookkeepingResult;
import com.echoamoy.holdlens.server.domain.bookkeeping.adapter.repository.IBookkeepingRepository;
import com.echoamoy.holdlens.server.domain.bookkeeping.adapter.repository.IBookkeepingCategoryRepository;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingBillEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingEntryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingStatisticsEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.entity.BookkeepingCategoryEntity;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingCategoryCatalog;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingEntryTypeEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj.BookkeepingGranularityEnumVO;
import com.echoamoy.holdlens.server.domain.bookkeeping.service.BookkeepingStatisticsService;
import com.echoamoy.holdlens.server.types.enums.ResponseCode;
import com.echoamoy.holdlens.server.types.exception.AppException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookkeepingCaseImpl implements IBookkeepingCase {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final BookkeepingStatisticsService statisticsService = new BookkeepingStatisticsService();

    @Resource
    private IBookkeepingRepository bookkeepingRepository;

    @Resource
    private IBookkeepingCategoryRepository categoryRepository;

    @Override
    public List<BookkeepingCategoryEntity> queryCategories(Long userId, String type) {
        BookkeepingEntryEntity.validateUserId(userId);
        return categoryRepository.queryVisible(userId, requireType(type)).stream()
                .filter(BookkeepingCategoryEntity::isEnabled)
                .toList();
    }

    @Override
    public BookkeepingResult.CategorySettings queryCategorySettings(Long userId, String type) {
        BookkeepingEntryEntity.validateUserId(userId);
        List<BookkeepingCategoryEntity> all = categoryRepository.queryVisible(userId, requireType(type));
        return BookkeepingResult.CategorySettings.builder()
                .enabled(all.stream().filter(BookkeepingCategoryEntity::isEnabled).toList())
                .disabled(all.stream().filter(category -> !category.isEnabled()).toList())
                .build();
    }

    @Override
    @Transactional
    public BookkeepingCategoryEntity createCategory(BookkeepingCommand.CreateCategory command) {
        if (command == null) {
            throw new IllegalArgumentException("类别创建参数不能为空");
        }
        BookkeepingEntryEntity.validateUserId(command.getUserId());
        BookkeepingEntryEntity.validateRequestId(command.getRequestId());

        String requestId = command.getRequestId().trim();
        BookkeepingCategoryEntity existed = categoryRepository.queryByOwnerAndRequestId(
                command.getUserId(),
                requestId
        );
        if (existed != null) {
            return existed;
        }

        BookkeepingEntryTypeEnumVO type = requireType(command.getType());
        String name = normalizeCategoryName(command.getName());
        if (!BookkeepingCategoryCatalog.isIconKey(command.getIconKey())) {
            throw new IllegalArgumentException("类别图标不合法");
        }

        List<BookkeepingCategoryEntity> visible = categoryRepository.queryVisible(command.getUserId(), type);
        if (visible.stream().anyMatch(category -> name.equals(category.getName()))) {
            throw new IllegalArgumentException("收支分类名称重复");
        }
        int sortOrder = visible.stream()
                .filter(BookkeepingCategoryEntity::isEnabled)
                .map(BookkeepingCategoryEntity::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 10;

        BookkeepingCategoryEntity category = BookkeepingCategoryEntity.builder()
                .code("CUS_" + UUID.randomUUID().toString().replace("-", "").toUpperCase())
                .scope("USER")
                .ownerUserId(command.getUserId())
                .type(type)
                .name(name)
                .iconKey(command.getIconKey())
                .defaultEnabled(true)
                .defaultSortOrder(sortOrder)
                .sortOrder(sortOrder)
                .createRequestId(requestId)
                .status("ENABLED")
                .activeEntryCount(0L)
                .build();
        boolean created = categoryRepository.insertUserCategory(category);
        if (!created) {
            return category;
        }
        categoryRepository.upsertConfig(command.getUserId(), category.getId(), "ENABLED", sortOrder);
        return category;
    }

    @Override
    @Transactional
    public BookkeepingCategoryEntity enableCategory(Long userId, String code) {
        BookkeepingEntryEntity.validateUserId(userId);
        BookkeepingCategoryEntity category = requireCategory(userId, code);
        if (!category.isEnabled()) {
            int lastOrder = categoryRepository.queryVisible(userId, category.getType()).stream()
                    .filter(BookkeepingCategoryEntity::isEnabled)
                    .map(BookkeepingCategoryEntity::getSortOrder)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0) + 10;
            categoryRepository.upsertConfig(userId, category.getId(), "ENABLED", lastOrder);
            category.setStatus("ENABLED");
            category.setSortOrder(lastOrder);
        }
        return category;
    }

    @Override
    @Transactional
    public int disableCategory(Long userId, String code) {
        BookkeepingEntryEntity.validateUserId(userId);
        BookkeepingCategoryEntity category = requireCategory(userId, code);
        if (!category.isEnabled()) {
            return 0;
        }

        categoryRepository.upsertConfig(userId, category.getId(), "DISABLED", category.getSortOrder());
        int deleted = categoryRepository.disableAndDeleteActiveEntries(
                userId,
                category.getType().name(),
                category.getCode()
        );
        List<BookkeepingCategoryEntity> enabled = queryCategories(userId, category.getType().name());
        for (int index = 0; index < enabled.size(); index++) {
            categoryRepository.upsertConfig(
                    userId,
                    enabled.get(index).getId(),
                    "ENABLED",
                    (index + 1) * 10
            );
        }
        return deleted;
    }

    @Override
    @Transactional
    public void reorderCategories(Long userId, String type, List<String> codes) {
        BookkeepingEntryEntity.validateUserId(userId);
        BookkeepingEntryTypeEnumVO entryType = requireType(type);
        List<BookkeepingCategoryEntity> current = queryCategories(userId, type);
        Set<String> submitted = codes == null ? Set.of() : new HashSet<>(codes);
        Set<String> expected = current.stream()
                .map(BookkeepingCategoryEntity::getCode)
                .collect(Collectors.toSet());
        if (codes == null || codes.size() != current.size()
                || submitted.size() != codes.size() || !submitted.equals(expected)) {
            throw new IllegalArgumentException("类别排序集合不一致");
        }

        Map<String, BookkeepingCategoryEntity> categories = current.stream()
                .collect(Collectors.toMap(BookkeepingCategoryEntity::getCode, category -> category));
        for (int index = 0; index < codes.size(); index++) {
            BookkeepingCategoryEntity category = categories.get(codes.get(index));
            if (category == null || category.getType() != entryType || !category.isEnabled()) {
                throw new IllegalArgumentException("类别排序集合不一致");
            }
            categoryRepository.upsertConfig(userId, category.getId(), "ENABLED", (index + 1) * 10);
        }
    }

    @Override
    @Transactional
    public BookkeepingEntryEntity create(BookkeepingCommand.Create command) {
        if (command == null) {
            throw new IllegalArgumentException("收支条目创建参数不能为空");
        }
        BookkeepingEntryEntity.validateUserId(command.getUserId());
        BookkeepingEntryEntity.validateRequestId(command.getRequestId());

        String requestId = command.getRequestId().trim();
        BookkeepingEntryEntity existed = bookkeepingRepository.queryByUserAndRequestId(command.getUserId(), requestId);
        if (existed != null) {
            return decorateEntry(existed);
        }

        BookkeepingEntryTypeEnumVO entryType = requireType(command.getType());
        requireEnabledCategory(command.getUserId(), command.getCategoryCode(), entryType);
        BookkeepingEntryEntity entry = BookkeepingEntryEntity.create(
                command.getUserId(),
                command.getRequestId(),
                entryType,
                command.getCategoryCode(),
                command.getAmount(),
                command.getEntryDate(),
                command.getNote()
        );
        bookkeepingRepository.insert(entry);
        return decorateEntry(queryPersistedEntry(command.getUserId(), requestId, entry));
    }

    @Override
    public BookkeepingEntryEntity queryEntry(Long userId, Long entryId) {
        return decorateEntry(requireEntry(userId, entryId));
    }

    @Override
    public BookkeepingResult.EntryList queryEntries(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            String type,
            String categoryCode
    ) {
        validateRange(userId, startDate, endDate);
        BookkeepingEntryTypeEnumVO resolvedType = resolveFilter(userId, type, categoryCode);
        List<BookkeepingEntryEntity> entries = bookkeepingRepository.queryActiveEntries(
                userId,
                startDate,
                endDate,
                resolvedType,
                categoryCode
        );
        BookkeepingStatisticsService.Totals totals = statisticsService.totals(entries);
        return BookkeepingResult.EntryList.builder()
                .startDate(startDate)
                .endDate(endDate)
                .income(totals.getIncome())
                .expense(totals.getExpense())
                .balance(totals.getBalance())
                .entries(decorateEntries(userId, entries))
                .build();
    }

    @Override
    @Transactional
    public BookkeepingEntryEntity revise(BookkeepingCommand.Revise command) {
        if (command == null) {
            throw new IllegalArgumentException("收支条目修订参数不能为空");
        }
        BookkeepingEntryEntity entry = requireEntry(command.getUserId(), command.getEntryId());
        BookkeepingEntryTypeEnumVO entryType = requireType(command.getType());
        requireEnabledCategory(command.getUserId(), command.getCategoryCode(), entryType);
        entry.revise(
                entryType,
                command.getCategoryCode(),
                command.getAmount(),
                command.getEntryDate(),
                command.getNote()
        );
        updateEntry(entry);
        BookkeepingEntryEntity persisted = bookkeepingRepository.queryActiveByUserAndId(
                command.getUserId(),
                command.getEntryId()
        );
        return decorateEntry(persisted == null ? entry : persisted);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long entryId) {
        BookkeepingEntryEntity entry = requireEntry(userId, entryId);
        entry.delete();
        updateEntry(entry);
    }

    @Override
    public BookkeepingResult.Statistics statistics(
            Long userId,
            String type,
            String granularity,
            LocalDate anchorDate
    ) {
        BookkeepingEntryEntity.validateUserId(userId);
        BookkeepingEntryTypeEnumVO entryType = requireType(type);
        BookkeepingGranularityEnumVO unit = requireGranularity(granularity);
        if (anchorDate == null) {
            throw new IllegalArgumentException("统计日期不合法");
        }

        LocalDate today = LocalDate.now(ZONE);
        BookkeepingStatisticsService.PeriodRange range = statisticsService.period(unit, anchorDate);
        if (range.start().isAfter(today)) {
            throw new IllegalArgumentException("统计周期尚未开始");
        }
        List<BookkeepingEntryEntity> entries = bookkeepingRepository.queryActiveEntries(
                userId,
                range.start(),
                range.end(),
                entryType,
                null
        );
        return toStatistics(userId, statisticsService.statistics(entryType, unit, range, today, entries));
    }

    @Override
    public BookkeepingResult.MonthlyBill monthlyBill(Long userId, Integer year) {
        BookkeepingEntryEntity.validateUserId(userId);
        LocalDate today = LocalDate.now(ZONE);
        if (year == null || year < 1 || year > today.getYear()) {
            throw new IllegalArgumentException("账单年份不合法");
        }
        List<BookkeepingEntryEntity> entries = bookkeepingRepository.queryActiveEntries(
                userId,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31),
                null,
                null
        );
        return toMonthlyBill(statisticsService.monthlyBill(year, today, entries));
    }

    @Override
    public BookkeepingResult.YearlyBill yearlyBill(Long userId) {
        BookkeepingEntryEntity.validateUserId(userId);
        List<BookkeepingBillEntity.Year> years = new ArrayList<>();
        for (Integer year : bookkeepingRepository.queryActiveYears(userId)) {
            List<BookkeepingEntryEntity> entries = bookkeepingRepository.queryActiveEntries(
                    userId,
                    LocalDate.of(year, 1, 1),
                    LocalDate.of(year, 12, 31),
                    null,
                    null
            );
            years.add(statisticsService.yearBill(year, entries));
        }
        return toYearlyBill(statisticsService.yearlyBill(years));
    }

    private BookkeepingEntryEntity queryPersistedEntry(
            Long userId,
            String requestId,
            BookkeepingEntryEntity fallback
    ) {
        BookkeepingEntryEntity persisted = bookkeepingRepository.queryByUserAndRequestId(userId, requestId);
        return persisted == null ? fallback : persisted;
    }

    private BookkeepingEntryEntity requireEntry(Long userId, Long entryId) {
        BookkeepingEntryEntity.validateUserId(userId);
        if (entryId == null || entryId <= 0) {
            throw new IllegalArgumentException("收支条目ID不合法");
        }
        BookkeepingEntryEntity entry = bookkeepingRepository.queryActiveByUserAndId(userId, entryId);
        if (entry == null) {
            throw invisibleEntry();
        }
        return entry;
    }

    private void updateEntry(BookkeepingEntryEntity entry) {
        try {
            bookkeepingRepository.update(entry);
        } catch (IllegalArgumentException exception) {
            throw invisibleEntry();
        }
    }

    private AppException invisibleEntry() {
        return new AppException(
                ResponseCode.ILLEGAL_PARAMETER.getCode(),
                "收支条目不存在或不可见"
        );
    }

    private BookkeepingEntryTypeEnumVO requireType(String type) {
        BookkeepingEntryTypeEnumVO result = BookkeepingEntryTypeEnumVO.from(type);
        if (result == null) {
            throw new IllegalArgumentException("收支类型不合法");
        }
        return result;
    }

    private BookkeepingGranularityEnumVO requireGranularity(String granularity) {
        BookkeepingGranularityEnumVO result = BookkeepingGranularityEnumVO.from(granularity);
        if (result == null) {
            throw new IllegalArgumentException("统计粒度不合法");
        }
        return result;
    }

    private BookkeepingEntryTypeEnumVO resolveFilter(Long userId, String type, String categoryCode) {
        BookkeepingEntryTypeEnumVO explicitType = type == null ? null : requireType(type);
        if (categoryCode == null) {
            return explicitType;
        }
        BookkeepingCategoryEntity category = categoryRepository.queryVisibleByCode(userId, categoryCode);
        if (category == null || (explicitType != null && category.getType() != explicitType)) {
            throw new IllegalArgumentException("收支分类与类型不匹配");
        }
        return category.getType();
    }

    private void validateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        BookkeepingEntryEntity.validateUserId(userId);
        if (startDate == null || endDate == null
                || startDate.isAfter(endDate)
                || startDate.plusDays(365).isBefore(endDate)) {
            throw new IllegalArgumentException("日期范围不合法");
        }
    }

    private BookkeepingCategoryEntity requireCategory(Long userId, String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("收支分类不合法");
        }
        BookkeepingCategoryEntity category = categoryRepository.queryVisibleByCode(userId, code.trim());
        if (category == null) {
            throw new IllegalArgumentException("收支分类不存在或不可见");
        }
        return category;
    }

    private void requireEnabledCategory(Long userId, String code, BookkeepingEntryTypeEnumVO type) {
        BookkeepingCategoryEntity category = requireCategory(userId, code);
        if (category.getType() != type || !category.isEnabled()) {
            throw new IllegalArgumentException("收支分类与类型不匹配或未启用");
        }
    }

    private String normalizeCategoryName(String raw) {
        String value = raw == null ? "" : raw.trim();
        int visibleCharacters = value.codePointCount(0, value.length());
        if (visibleCharacters < 1 || visibleCharacters > 4
                || value.codePoints().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("收支分类名称不合法");
        }
        return value;
    }

    private BookkeepingEntryEntity decorateEntry(BookkeepingEntryEntity entry) {
        if (entry == null) {
            return null;
        }
        BookkeepingCategoryEntity category = requireCategory(entry.getUserId(), entry.getCategoryCode());
        entry.setCategoryName(category.getName());
        entry.setCategoryIconKey(category.getIconKey());
        return entry;
    }

    private List<BookkeepingEntryEntity> decorateEntries(
            Long userId,
            List<BookkeepingEntryEntity> entries
    ) {
        Map<BookkeepingEntryTypeEnumVO, Map<String, BookkeepingCategoryEntity>> byType = new HashMap<>();
        for (BookkeepingEntryEntity entry : entries) {
            Map<String, BookkeepingCategoryEntity> categories = byType.computeIfAbsent(
                    entry.getType(),
                    type -> visibleCategoryMap(userId, type)
            );
            BookkeepingCategoryEntity category = categories.get(entry.getCategoryCode());
            if (category == null) {
                throw new IllegalArgumentException("收支分类不存在或不可见");
            }
            entry.setCategoryName(category.getName());
            entry.setCategoryIconKey(category.getIconKey());
        }
        return entries;
    }

    private Map<String, BookkeepingCategoryEntity> visibleCategoryMap(
            Long userId,
            BookkeepingEntryTypeEnumVO type
    ) {
        return categoryRepository.queryVisible(userId, type).stream()
                .collect(Collectors.toMap(BookkeepingCategoryEntity::getCode, category -> category));
    }

    private BookkeepingResult.Statistics toStatistics(Long userId, BookkeepingStatisticsEntity value) {
        Map<String, BookkeepingCategoryEntity> definitions = visibleCategoryMap(userId, value.getType());
        return BookkeepingResult.Statistics.builder()
                .type(value.getType())
                .granularity(value.getGranularity().name())
                .periodStart(value.getPeriodStart())
                .periodEnd(value.getPeriodEnd())
                .total(value.getTotal())
                .average(value.getAverage())
                .points(value.getPoints().stream().map(point -> BookkeepingResult.Point.builder()
                        .periodStart(point.getPeriodStart())
                        .periodEnd(point.getPeriodEnd())
                        .label(point.getLabel())
                        .amount(point.getAmount())
                        .build()).toList())
                .categories(value.getCategories().stream().map(category -> {
                    BookkeepingCategoryEntity definition = definitions.get(category.getCategoryCode());
                    if (definition == null) {
                        throw new IllegalArgumentException("收支分类不存在或不可见");
                    }
                    return BookkeepingResult.Category.builder()
                            .categoryCode(category.getCategoryCode())
                            .categoryName(definition.getName())
                            .categoryIconKey(definition.getIconKey())
                            .amount(category.getAmount())
                            .ratio(category.getRatio())
                            .build();
                }).toList())
                .build();
    }

    private BookkeepingResult.MonthlyBill toMonthlyBill(BookkeepingBillEntity.Monthly value) {
        return BookkeepingResult.MonthlyBill.builder()
                .year(value.getYear())
                .income(value.getIncome())
                .expense(value.getExpense())
                .balance(value.getBalance())
                .months(value.getMonths().stream().map(month -> BookkeepingResult.Month.builder()
                        .month(month.getMonth())
                        .income(month.getIncome())
                        .expense(month.getExpense())
                        .balance(month.getBalance())
                        .build()).toList())
                .build();
    }

    private BookkeepingResult.YearlyBill toYearlyBill(BookkeepingBillEntity.Yearly value) {
        return BookkeepingResult.YearlyBill.builder()
                .income(value.getIncome())
                .expense(value.getExpense())
                .balance(value.getBalance())
                .years(value.getYears().stream().map(year -> BookkeepingResult.Year.builder()
                        .year(year.getYear())
                        .income(year.getIncome())
                        .expense(year.getExpense())
                        .balance(year.getBalance())
                        .build()).toList())
                .build();
    }
}
