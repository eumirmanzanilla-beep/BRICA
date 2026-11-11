package com.brica.documents.infrastructure;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ReferenceFieldMigrationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Test
    void shouldUpgradeV4WithoutModifyingExistingBusinessRows() {
        var dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).target("4").load().migrate();
        Long clientId = jdbc.queryForObject("""
                INSERT INTO cliente (nombre, representante_legal)
                VALUES ('Cliente previo', 'Representante') RETURNING id_cliente
                """, Long.class);
        Long workId = jdbc.queryForObject("""
                INSERT INTO works (client_id, name, location, start_date)
                VALUES (?, 'Obra previa', 'Mérida', '2026-01-01') RETURNING id_work
                """, Long.class, clientId);
        jdbc.update("INSERT INTO contracts (work_id, data) VALUES (?, '{\"preserved\":true}'::jsonb)", workId);
        var before = snapshot(jdbc);
        var result = Flyway.configure().dataSource(dataSource).target("5").load().migrate();
        assertEquals(1, result.migrationsExecuted);
        assertEquals("5", result.targetSchemaVersion);
        assertEquals(before, snapshot(jdbc));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM reference_fields", Integer.class));
        assertEquals(50, jdbc.queryForObject("""
                SELECT character_maximum_length FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'reference_fields' AND column_name = 'key'
                """, Integer.class));
        assertEquals(0, Flyway.configure().dataSource(dataSource).target("5").load().migrate().migrationsExecuted);
        var security = Flyway.configure().dataSource(dataSource).target("6").load().migrate();
        assertEquals(1, security.migrationsExecuted);
        assertEquals("6", security.targetSchemaVersion);
        assertEquals(before, snapshot(jdbc));
        assertEquals(8, jdbc.queryForObject("SELECT count(*) FROM permissions", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM app_users", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM roles", Integer.class));
        var documents = Flyway.configure().dataSource(dataSource).load().migrate();
        assertEquals(1, documents.migrationsExecuted);
        assertEquals("7", documents.targetSchemaVersion);
        assertEquals(before, snapshot(jdbc));
        assertEquals(54, jdbc.queryForObject("""
                SELECT character_maximum_length FROM information_schema.columns
                WHERE table_name = 'template_version_fields' AND column_name = 'marker'
                """, Integer.class));
    }

    private List<String> snapshot(JdbcTemplate jdbc) {
        return List.of(
                jdbc.queryForObject("SELECT row_to_json(c)::text FROM cliente c", String.class),
                jdbc.queryForObject("SELECT row_to_json(w)::text FROM works w", String.class),
                jdbc.queryForObject("SELECT row_to_json(c)::text FROM contracts c", String.class));
    }
}
