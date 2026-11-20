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
        var documents = Flyway.configure().dataSource(dataSource).target("7").load().migrate();
        assertEquals(1, documents.migrationsExecuted);
        assertEquals("7", documents.targetSchemaVersion);
        assertEquals(before, snapshot(jdbc));
        assertEquals(54, jdbc.queryForObject("""
                SELECT character_maximum_length FROM information_schema.columns
                WHERE table_name = 'template_version_fields' AND column_name = 'marker'
                """, Integer.class));
        var expansion = Flyway.configure().dataSource(dataSource).target("8").load().migrate();
        assertEquals(1, expansion.migrationsExecuted);
        assertEquals("8", expansion.targetSchemaVersion);
        assertEquals("IN_PROGRESS", jdbc.queryForObject("SELECT status FROM works WHERE id_work = ?", String.class, workId));
        var names = Flyway.configure().dataSource(dataSource).target("9").load().migrate();
        assertEquals(1, names.migrationsExecuted);
        assertEquals("9", names.targetSchemaVersion);
        var roles = Flyway.configure().dataSource(dataSource).target("10").load().migrate();
        assertEquals(1, roles.migrationsExecuted);
        assertEquals("10", roles.targetSchemaVersion);
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM roles", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM app_users", Integer.class));
        assertEquals(8, jdbc.queryForObject("SELECT count(*) FROM role_permissions rp JOIN roles r ON r.id_role=rp.role_id WHERE r.name='Gerencial'", Integer.class));
        assertEquals(8, jdbc.queryForObject("SELECT count(*) FROM role_permissions rp JOIN roles r ON r.id_role=rp.role_id WHERE r.name='Administrativo'", Integer.class));
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM role_permissions rp JOIN roles r ON r.id_role=rp.role_id WHERE r.name='Practicante'", Integer.class));
        assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM role_permissions rp JOIN roles r ON r.id_role=rp.role_id WHERE r.name='Invitado'", Integer.class));
        assertEquals(List.of("Administrativo:3", "Gerencial:4", "Invitado:1", "Practicante:2"),
                jdbc.queryForList("SELECT name || ':' || hierarchy_level FROM roles ORDER BY name", String.class));
        var readOnly = List.of("CLIENTS_READ", "CONTRACTS_READ", "DOCUMENT_VALUES_READ", "WORKS_READ");
        for (String role : List.of("Practicante", "Invitado")) {
            assertEquals(readOnly, jdbc.queryForList("""
                    SELECT p.key FROM permissions p JOIN role_permissions rp ON rp.permission_id=p.id_permission
                    JOIN roles r ON r.id_role=rp.role_id WHERE r.name=? ORDER BY p.key
                    """, String.class, role));
        }
        var navigation = Flyway.configure().dataSource(dataSource).target("14").load().migrate();
        assertEquals(4, navigation.migrationsExecuted);
        assertEquals("14", navigation.targetSchemaVersion);
        assertEquals(17, jdbc.queryForObject("SELECT count(*) FROM reference_fields", Integer.class));
        assertEquals("Obra previa", jdbc.queryForObject("SELECT name FROM works WHERE id_work=?", String.class, workId));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM work_attachments", Integer.class));
    }

    private List<String> snapshot(JdbcTemplate jdbc) {
        return List.of(
                jdbc.queryForObject("SELECT row_to_json(c)::text FROM cliente c", String.class),
                jdbc.queryForObject("SELECT row_to_json(w)::text FROM works w", String.class),
                jdbc.queryForObject("SELECT row_to_json(c)::text FROM contracts c", String.class));
    }
}
