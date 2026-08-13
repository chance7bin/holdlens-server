package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BookkeepingCategoryPersistenceSqlStructureTest {

    private static final Pattern SYSTEM_CATEGORY_ROW = Pattern.compile(
            "\\('(?:FOOD|SHOPPING|DAILY|TRANSPORT|VEGETABLE|FRUIT|SNACK|SPORT|ENTERTAINMENT|"
                    + "COMMUNICATION|CLOTHING|BEAUTY|HOUSING|HOME|MEDICAL|OTHER_EXPENSE|SALARY|"
                    + "BONUS|PART_TIME|BUSINESS|INVESTMENT_INCOME|REIMBURSEMENT|OTHER_INCOME)'"
    );

    @Test
    public void mapperAndMigrationKeepIsolationAndCompatibility() throws Exception {
        Path root = Path.of("..").toAbsolutePath().normalize();
        String mapper = Files.readString(root.resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/bookkeeping_category_mapper.xml"
        ));
        String initialization = Files.readString(root.resolve("docs/dev-ops/mysql/sql/holdlens.sql"));
        String migration = Files.readString(root.resolve(
                "docs/dev-ops/mysql/sql/migrations/20260813_manage_bookkeeping_categories.sql"
        ));

        assertTrue(mapper.contains("cfg.user_id = #{userId}"));
        assertTrue(mapper.contains("c.owner_user_id = #{userId}"));
        assertTrue(mapper.contains("UPDATE bookkeeping_entry"));
        assertTrue(mapper.contains("category_code = #{categoryCode}"));
        assertTrue(mapper.contains("status = 'ACTIVE'"));
        assertTrue(mapper.contains("FOR UPDATE"));
        assertFalse(mapper.contains("${"));

        assertTrue(initialization.contains("uk_bookkeeping_category_owner_type_name"));
        assertTrue(initialization.contains("PRIMARY KEY (`user_id`, `category_id`)"));
        assertEquals(23, countSystemCategories(initialization));

        assertTrue(migration.contains("INSERT IGNORE INTO `bookkeeping_category`"));
        assertEquals(23, countSystemCategories(migration));
        assertTrue(migration.contains("e.status = 'ACTIVE'"));
        assertTrue(migration.contains("c.default_enabled = 0"));
        assertFalse(migration.contains("UPDATE bookkeeping_entry"));
    }

    private int countSystemCategories(String sql) {
        Matcher matcher = SYSTEM_CATEGORY_ROW.matcher(sql);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
