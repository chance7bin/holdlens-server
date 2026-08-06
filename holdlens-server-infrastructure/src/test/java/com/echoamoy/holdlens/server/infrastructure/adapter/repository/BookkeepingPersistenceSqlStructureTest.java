package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BookkeepingPersistenceSqlStructureTest {

    @Test
    public void mapperAndSchemaKeepIsolationAndIndexes() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        if (!Files.exists(root.resolve("holdlens-server-app"))) {
            root = root.getParent();
        }
        String mapper = Files.readString(root.resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/bookkeeping_entry_mapper.xml"
        ));
        String ddl = Files.readString(root.resolve("docs/dev-ops/mysql/sql/holdlens.sql"));

        assertTrue(mapper.contains("user_id = #{userId}"));
        assertTrue(mapper.contains("status = 'ACTIVE'"));
        assertTrue(mapper.contains("ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)"));
        assertFalse(mapper.contains("${"));
        assertTrue(ddl.contains("uk_bookkeeping_entry_user_request"));
        assertTrue(ddl.contains("idx_bookkeeping_entry_detail"));
    }
}
