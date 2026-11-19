package com.brica.documents.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class VersionLifecycleMigrationIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");
    @Test void upgradesV13WithoutChangingFilesBindingsOrVersionNumbers() {
        var dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var jdbc = new JdbcTemplate(dataSource);
        Flyway.configure().dataSource(dataSource).target("13").load().migrate();
        long template = jdbc.queryForObject("INSERT INTO document_templates(name, document_type, state) VALUES ('Historica', 'ACTA', 'CURRENT') RETURNING id_template", Long.class);
        long file = jdbc.queryForObject("INSERT INTO document_files(original_name,relative_path,mime_type,size_bytes,sha256,registered_at) VALUES ('original.docx','original.bin','application/octet-stream',1,repeat('a',64),now()) RETURNING id_file", Long.class);
        jdbc.update("INSERT INTO template_versions VALUES (?,7,?,false,'{}'::jsonb)", template, file);
        long field = jdbc.queryForObject("SELECT id_reference_field FROM reference_fields WHERE key='NOMBRE_OBRA'", Long.class);
        jdbc.update("INSERT INTO template_version_fields VALUES (?,7,?,true,'{{NOMBRE_OBRA}}')", template, field);
        jdbc.update("INSERT INTO document_registrations(confirmation_id,template_id,request_hash,file_hash,original_name,size_bytes,analysis_json,requirements_json,status,version_number) VALUES ('00000000-0000-0000-0000-000000000001',?,repeat('b',64),repeat('a',64),'original.docx',1,'{}','{}','COMPLETED',7)", template);
        String before = jdbc.queryForObject("SELECT row_to_json(f)::text FROM document_files f WHERE id_file=?", String.class, file);
        var migrated = Flyway.configure().dataSource(dataSource).target("14").load().migrate();
        assertEquals(1, migrated.migrationsExecuted);
        assertEquals("Version_7", jdbc.queryForObject("SELECT name FROM template_versions WHERE template_id=? AND version_number=7", String.class, template));
        assertEquals("Version_7", jdbc.queryForObject("SELECT version_name FROM document_registrations WHERE template_id=?", String.class, template));
        assertEquals(before, jdbc.queryForObject("SELECT row_to_json(f)::text FROM document_files f WHERE id_file=?", String.class, file));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM template_version_fields WHERE template_id=? AND version_number=7 AND field_id=?", Integer.class, template, field));
        assertNull(jdbc.queryForObject("SELECT deleted_at FROM template_versions WHERE template_id=?", java.time.OffsetDateTime.class, template));
    }
}
