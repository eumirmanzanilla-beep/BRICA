package com.brica.documents.infrastructure;

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
class TemplateNameMigrationIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Test void refusesExactDuplicatesWithoutRenamingOrDeletingHistoricalRows() {
        var ds = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var jdbc = new JdbcTemplate(ds);
        Flyway.configure().dataSource(ds).target("8").load().migrate();
        jdbc.update("INSERT INTO document_templates (name, document_type, state) VALUES ('Duplicada', 'ACTA', 'DRAFT')");
        jdbc.update("INSERT INTO document_templates (name, document_type, state, deleted_at) VALUES ('Duplicada', 'OTRO', 'DRAFT', now())");
        var before = jdbc.queryForList("SELECT row_to_json(t)::text FROM document_templates t ORDER BY id_template", String.class);
        assertThrows(FlywayException.class, () -> Flyway.configure().dataSource(ds).target("9").load().migrate());
        assertEquals(before, jdbc.queryForList("SELECT row_to_json(t)::text FROM document_templates t ORDER BY id_template", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE version='9'", Integer.class));
    }
}
