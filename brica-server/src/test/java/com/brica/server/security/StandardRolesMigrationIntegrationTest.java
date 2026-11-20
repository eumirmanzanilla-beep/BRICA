package com.brica.server.security;

import java.util.Set;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class StandardRolesMigrationIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Test void refusesConflictingRolesWithoutChangingExistingAccounts() {
        var ds = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var jdbc = new JdbcTemplate(ds);
        Flyway.configure().dataSource(ds).target("9").load().migrate();
        Long id = jdbc.queryForObject("""
                INSERT INTO roles (name, hierarchy_level, status) VALUES ('Gerencial', 99, 1) RETURNING id_role
                """, Long.class);
        jdbc.update("INSERT INTO app_users (role_id, username, password_hash, name, status) VALUES (?, 'previo', 'hash', 'Previo', 'ACTIVE')", id);
        assertThrows(FlywayException.class, () -> Flyway.configure().dataSource(ds).load().migrate());
        assertEquals(99, jdbc.queryForObject("SELECT hierarchy_level FROM roles WHERE id_role=?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM app_users WHERE username='previo' AND role_id=?", Integer.class, id));
        assertEquals(Set.of("Gerencial"), Set.copyOf(jdbc.queryForList("SELECT name FROM roles", String.class)));
    }
}
