package com.echoamoy.holdlens.server.domain.bookkeeping.model.valobj;

import java.util.Arrays;
import java.util.List;

public enum BookkeepingCategoryEnumVO {
    FOOD(BookkeepingEntryTypeEnumVO.EXPENSE, "餐饮", 10),
    SHOPPING(BookkeepingEntryTypeEnumVO.EXPENSE, "购物", 20),
    DAILY(BookkeepingEntryTypeEnumVO.EXPENSE, "日用", 30),
    TRANSPORT(BookkeepingEntryTypeEnumVO.EXPENSE, "交通", 40),
    VEGETABLE(BookkeepingEntryTypeEnumVO.EXPENSE, "买菜", 50),
    FRUIT(BookkeepingEntryTypeEnumVO.EXPENSE, "水果", 60),
    SNACK(BookkeepingEntryTypeEnumVO.EXPENSE, "零食", 70),
    SPORT(BookkeepingEntryTypeEnumVO.EXPENSE, "运动", 80),
    ENTERTAINMENT(BookkeepingEntryTypeEnumVO.EXPENSE, "娱乐", 90),
    COMMUNICATION(BookkeepingEntryTypeEnumVO.EXPENSE, "通讯", 100),
    CLOTHING(BookkeepingEntryTypeEnumVO.EXPENSE, "服饰", 110),
    BEAUTY(BookkeepingEntryTypeEnumVO.EXPENSE, "美妆", 120),
    HOUSING(BookkeepingEntryTypeEnumVO.EXPENSE, "住房", 130),
    HOME(BookkeepingEntryTypeEnumVO.EXPENSE, "家居", 140),
    MEDICAL(BookkeepingEntryTypeEnumVO.EXPENSE, "医疗", 150),
    OTHER_EXPENSE(BookkeepingEntryTypeEnumVO.EXPENSE, "其他支出", 160),
    SALARY(BookkeepingEntryTypeEnumVO.INCOME, "工资", 10),
    BONUS(BookkeepingEntryTypeEnumVO.INCOME, "奖金", 20),
    PART_TIME(BookkeepingEntryTypeEnumVO.INCOME, "兼职", 30),
    BUSINESS(BookkeepingEntryTypeEnumVO.INCOME, "经营", 40),
    INVESTMENT_INCOME(BookkeepingEntryTypeEnumVO.INCOME, "投资收益", 50),
    REIMBURSEMENT(BookkeepingEntryTypeEnumVO.INCOME, "报销", 60),
    OTHER_INCOME(BookkeepingEntryTypeEnumVO.INCOME, "其他收入", 70);

    private final BookkeepingEntryTypeEnumVO type;
    private final String name;
    private final int sortOrder;

    BookkeepingCategoryEnumVO(BookkeepingEntryTypeEnumVO type, String name, int sortOrder) {
        this.type = type;
        this.name = name;
        this.sortOrder = sortOrder;
    }

    public BookkeepingEntryTypeEnumVO getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public static BookkeepingCategoryEnumVO require(String code, BookkeepingEntryTypeEnumVO type) {
        BookkeepingCategoryEnumVO category;
        try {
            category = code == null ? null : valueOf(code);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("收支分类不合法");
        }
        if (category == null || category.type != type) {
            throw new IllegalArgumentException("收支分类与类型不匹配");
        }
        return category;
    }

    public static BookkeepingCategoryEnumVO require(String code) {
        if (code == null) {
            throw new IllegalArgumentException("收支分类不合法");
        }
        try {
            return valueOf(code);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("收支分类不合法");
        }
    }

    public static List<BookkeepingCategoryEnumVO> byType(BookkeepingEntryTypeEnumVO type) {
        return Arrays.stream(values()).filter(item -> item.type == type).toList();
    }
}
