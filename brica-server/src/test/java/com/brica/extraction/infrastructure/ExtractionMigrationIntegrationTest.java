package com.brica.extraction.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ExtractionMigrationIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:18-alpine");
    private DriverManagerDataSource dataSource(String schema) {
        String url=postgres.getJdbcUrl();
        return new DriverManagerDataSource(url+(url.contains("?")?"&":"?")+"currentSchema="+schema,postgres.getUsername(),postgres.getPassword());
    }
    private Flyway flyway(DriverManagerDataSource ds,String schema,String target) {
        return Flyway.configure().dataSource(ds).schemas(schema).defaultSchema(schema).target(target).load();
    }
    @Test void upgradesV14WithoutInventingDocumentNamesOrChangingPriorBusinessRecords() {
        String schema="upgrade_"+UUID.randomUUID().toString().replace("-",""); var ds=dataSource(schema);
        flyway(ds,schema,"14").migrate(); var jdbc=new JdbcTemplate(ds);
        long client=jdbc.queryForObject("insert into cliente(nombre,representante_legal) values('Cliente histórico','Representante original') returning id_cliente",Long.class);
        long work=jdbc.queryForObject("insert into works(client_id,name,description) values(?,'Obra histórica','Descripción intacta') returning id_work",Long.class,client);
        jdbc.update("insert into work_attachments(work_id,kind,original_name,mime_type,relative_path,size_bytes,sha256,registered_at) values(?,'CONTRACT','fuente.pdf','application/pdf','registrations/historical.docx',1,repeat('a',64),now())",work);
        String before=snapshot(jdbc,work);
        var migrated=flyway(ds,schema,"18").migrate(); assertEquals(4,migrated.migrationsExecuted);
        assertEquals(before,snapshot(jdbc,work));
        assertNull(jdbc.queryForObject("select document_name from works where id_work=?",String.class,work));
        assertNull(jdbc.queryForObject("select document_name from cliente where id_cliente=?",String.class,client));
        assertEquals(0L,jdbc.queryForObject("select revision from cliente where id_cliente=?",Long.class,client));
        assertEquals("name",jdbc.queryForObject("select source_path from reference_fields where key='NOMBRE_OBRA'",String.class));
        assertEquals("documentName",jdbc.queryForObject("select source_path from reference_fields where key='DENOMINACION_OBRA'",String.class));
        assertEquals(0,jdbc.queryForObject("select count(*) from extraction_jobs",Integer.class));
        assertEquals(0,jdbc.queryForObject("select count(*) from budgets",Integer.class));
    }
    private String snapshot(JdbcTemplate jdbc,long work) {
        return jdbc.queryForObject("select jsonb_build_object('client',to_jsonb(c)-'document_name'-'revision','work',to_jsonb(w)-'document_name','attachment',to_jsonb(a))::text from works w join cliente c on c.id_cliente=w.client_id join work_attachments a on a.work_id=w.id_work where w.id_work=?",String.class,work);
    }
    @Test void importsExistingAttemptsAsExplicitLegacyHistoryWithoutInventedTimestamps() {
        String schema="history_"+UUID.randomUUID().toString().replace("-",""); var ds=dataSource(schema);
        flyway(ds,schema,"17").migrate(); var jdbc=new JdbcTemplate(ds);
        long client=jdbc.queryForObject("insert into cliente(nombre,representante_legal) values('Cliente','Representante') returning id_cliente",Long.class);
        long work=jdbc.queryForObject("insert into works(client_id,name) values(?,'Obra') returning id_work",Long.class,client);
        UUID job=UUID.randomUUID(),token=UUID.randomUUID();
        jdbc.update("insert into extraction_jobs(id,work_id,client_id,request_key,request_hash,actor,state,review_state,review_data,result_data,attempt_token,attempt_number,lease_until,created_at,updated_at) values(?,?,?,?,repeat('a',64),'legacy','RUNNING','DRAFT','{}','[]',?,2,now()+interval '30 seconds',now(),now())",job,work,client,UUID.randomUUID(),token);
        flyway(ds,schema,"18").migrate();
        assertEquals("RUNNING",jdbc.queryForObject("select outcome from extraction_attempts where id=?",String.class,token));
        assertTrue(jdbc.queryForObject("select legacy_imported from extraction_attempts where id=?",Boolean.class,token));
        assertNull(jdbc.queryForObject("select started_at::text from extraction_attempts where id=?",String.class,token));
        assertNull(jdbc.queryForObject("select result_hash from extraction_attempts where id=?",String.class,token));
        jdbc.update("update extraction_attempts set outcome='EXPIRED',finished_at=now() where id=?",token);
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("update extraction_attempts set outcome='COMPLETED' where id=?",token));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("delete from extraction_attempts where id=?",token));
    }
}
