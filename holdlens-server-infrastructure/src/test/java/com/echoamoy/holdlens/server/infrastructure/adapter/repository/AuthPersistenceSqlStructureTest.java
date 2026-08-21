package com.echoamoy.holdlens.server.infrastructure.adapter.repository;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthPersistenceSqlStructureTest {

    @Test
    public void mapperUsesBoundParametersAndSessionActiveFilter() throws Exception {
        Path root = projectRoot();
        String accountMapper = Files.readString(root.resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/user_account_mapper.xml"));
        String sessionMapper = Files.readString(root.resolve(
                "holdlens-server-app/src/main/resources/mybatis/mapper/user_session_mapper.xml"));

        assertTrue(accountMapper.contains("username = #{username}"));
        assertTrue(accountMapper.contains("<select id=\"selectByUsernameForUpdate\""));
        assertTrue(accountMapper.contains("FOR UPDATE"));
        assertTrue(accountMapper.contains("failed_login_count = CASE"));
        assertTrue(sessionMapper.contains("token_hash = #{tokenHash}"));
        assertTrue(sessionMapper.contains("installation_id, device_name"));
        assertTrue(sessionMapper.contains("#{installationId}, #{deviceName}"));
        assertTrue(sessionMapper.contains("revoked_at IS NULL"));
        assertTrue(sessionMapper.contains("expires_at &gt; CURRENT_TIMESTAMP"));
        assertTrue(sessionMapper.contains("COALESCE(revoked_at, CURRENT_TIMESTAMP)"));
        assertTrue(sessionMapper.contains("<select id=\"selectByIdForUpdate\""));
        assertTrue(sessionMapper.contains("<update id=\"revokeActiveByUserId\""));
        assertTrue(sessionMapper.contains("user_id = #{userId}"));
        assertTrue(sessionMapper.contains("<update id=\"updateExpiresAt\""));
        assertTrue(sessionMapper.contains("FOR UPDATE"));
        assertFalse(accountMapper.contains("${"));
        assertFalse(sessionMapper.contains("${"));
    }

    @Test
    public void schemaAndMigrationReserveFixedUserWithoutBusinessDataChanges() throws Exception {
        Path root = projectRoot();
        String ddl = Files.readString(root.resolve("docs/dev-ops/mysql/sql/holdlens.sql"));
        String migration = Files.readString(root.resolve(
                "docs/dev-ops/mysql/sql/migrations/20260819_self_hosted_user_authentication.sql"));
        String deviceMigration = Files.readString(root.resolve(
                "docs/dev-ops/mysql/sql/migrations/20260821_add_user_session_installation_identity.sql"));

        assertTrue(ddl.contains("CREATE TABLE `user_account`"));
        assertTrue(ddl.contains("AUTO_INCREMENT=2"));
        assertTrue(ddl.contains("uk_user_account_username"));
        assertTrue(ddl.contains("CREATE TABLE `user_session`"));
        assertTrue(ddl.contains("uk_user_session_token_hash"));
        assertTrue(ddl.contains("installation_id"));
        assertTrue(ddl.contains("idx_user_session_user_installation"));
        assertTrue(migration.contains("ALTER TABLE `user_account` AUTO_INCREMENT = 2"));
        assertTrue(deviceMigration.contains("ADD COLUMN `installation_id`"));
        assertTrue(deviceMigration.contains("ADD COLUMN `device_name`"));
        assertTrue(deviceMigration.contains("idx_user_session_user_installation"));
        assertFalse(migration.contains("INSERT INTO `asset_"));
        assertFalse(migration.contains("UPDATE `asset_"));
    }

    private Path projectRoot() {
        Path root = Path.of("").toAbsolutePath();
        return Files.exists(root.resolve("holdlens-server-app")) ? root : root.getParent();
    }
}
