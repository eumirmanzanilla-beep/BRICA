package com.brica.documents.infrastructure;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.brica.clients.domain.Client;
import com.brica.clients.infrastructure.ClientRepository;
import com.brica.contracts.domain.Contract;
import com.brica.contracts.infrastructure.ContractRepository;
import com.brica.documents.application.DuplicateReferenceKeyException;
import com.brica.documents.application.ListReferenceFieldsService;
import com.brica.documents.application.RegisterReferenceFieldService;
import com.brica.documents.application.ResolveReferenceValuesService;
import com.brica.documents.application.UpdateReferenceFieldService;
import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.domain.ReferenceAttribute;
import com.brica.documents.domain.ReferenceField;
import com.brica.documents.domain.ReferenceOrigin;
import com.brica.documents.domain.ReferenceValue;
import com.brica.works.application.WorkNotFoundException;
import com.brica.works.domain.Work;
import com.brica.works.infrastructure.WorkRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@org.springframework.security.test.context.support.WithMockUser(username = "IntegrationAdmin")
class ReferenceFieldPersistenceIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired private RegisterReferenceFieldService register;
    @Autowired private UpdateReferenceFieldService update;
    @Autowired private ListReferenceFieldsService list;
    @Autowired private ResolveReferenceValuesService resolve;
    @Autowired private ReferenceFieldStore store;
    @Autowired private ReferenceFieldRepository fields;
    @Autowired private ClientRepository clients;
    @Autowired private WorkRepository works;
    @Autowired private ContractRepository contracts;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwords;

    @org.junit.jupiter.api.BeforeEach
    void prepareSecurityAccount() {
        com.brica.server.security.SecurityTestData.installAdmin(jdbc, passwords);
    }

    @Test
    @Transactional
    void shouldPersistAndRenameWithoutChangingMeaningOrBusinessData() {
        var work = createWork("Obra A", "Cliente A");
        contracts.saveAndFlush(new Contract(work, Map.of("source", "preserve")));
        var before = businessSnapshot(work);
        var field = register.register("A".repeat(50), "Nombre", ReferenceAttribute.WORK_NAME);
        Long id = field.getId();
        Long revision = field.getRevision();
        entityManager.clear();
        update.rename(id, "Nombre actualizado");
        entityManager.clear();
        var reloaded = fields.findById(id).orElseThrow();
        assertEquals("Nombre actualizado", reloaded.getVisibleName());
        assertEquals("A".repeat(50), reloaded.getKey());
        assertEquals(ReferenceAttribute.WORK_NAME, reloaded.getAttribute());
        assertTrue(reloaded.getRevision() > revision);
        assertEquals(before, businessSnapshot(work));
        var result = resolve.resolveForWork(work.getId(), List.of(id)).getFirst();
        assertEquals(54, result.marker().length());
        assertEquals("Obra A", result.textValue());
    }

    @Test
    @Transactional
    void shouldResolveFreshNamesForEachWorkAndOnlyItsAssociatedClient() {
        var workField = register.register("OBRA", "Obra", ReferenceAttribute.WORK_NAME);
        var clientField = register.register("CLIENTE", "Cliente", ReferenceAttribute.CLIENT_NAME);
        var first = createWork("Obra A", "Cliente A");
        var second = createWork("Obra B", "Cliente B");
        var ids = List.of(workField.getId(), clientField.getId());
        assertEquals(List.of("Obra A", "Cliente A"), texts(resolve.resolveForWork(first.getId(), ids)));
        assertEquals(List.of("Obra B", "Cliente B"), texts(resolve.resolveForWork(second.getId(), ids)));
        first.correctGeneralInformation("Obra corregida", first.getLocation(), first.getDescription());
        first.getClient().updateDetails("Cliente corregido", "Representante");
        entityManager.flush();
        entityManager.clear();
        var current = resolve.resolveForWork(first.getId(), ids);
        assertEquals(List.of("Obra corregida", "Cliente corregido"), texts(current));
        assertEquals(ReferenceOrigin.CLIENT, current.getLast().origin());
        assertEquals(List.of("Obra B", "Cliente B"), texts(resolve.resolveForWork(second.getId(), ids)));
    }

    @Test
    @Transactional
    void shouldRejectDeletedWorkWithoutDeletingItsContract() {
        var work = createWork("Eliminada", "Cliente");
        var contract = contracts.saveAndFlush(new Contract(work, Map.of("historical", true)));
        work.markDeleted(OffsetDateTime.now());
        works.saveAndFlush(work);
        assertThrows(WorkNotFoundException.class,
                () -> resolve.resolveForWork(work.getId(), List.of()));
        assertTrue(contracts.existsById(contract.getId()));
    }

    @Test
    @Transactional
    void shouldRejectDeletedClientWithoutDeletingWorkOrContract() {
        var work = createWork("Obra", "Cliente eliminado");
        var contract = contracts.saveAndFlush(new Contract(work, Map.of("historical", true)));
        work.getClient().markDeleted(OffsetDateTime.now());
        clients.saveAndFlush(work.getClient());
        assertThrows(WorkNotFoundException.class,
                () -> resolve.resolveForWork(work.getId(), List.of()));
        assertTrue(works.existsById(work.getId()));
        assertTrue(contracts.existsById(contract.getId()));
    }

    @Test
    @Transactional
    void shouldPaginateFieldsInStableIdentifierOrder() {
        var first = register.register("FIRST", "Z", ReferenceAttribute.WORK_NAME);
        var second = register.register("SECOND", "A", ReferenceAttribute.CLIENT_NAME);
        var all = list.list(0, 200);
        assertTrue(all.indexOf(first) < all.indexOf(second));
        assertEquals(all.getFirst().getId(), list.list(0, 1).getFirst().getId());
        assertEquals(all.get(1).getId(), list.list(1, 1).getFirst().getId());
    }

    @Test
    void shouldRejectUnapprovedDefinitionsAndInvalidKeysInDatabase() {
        for (String key : new String[] {"lowercase", "WITH SPACE", "A".repeat(51), "OBRA\n"}) {
            assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                    INSERT INTO reference_fields (key, visible_name, data_type, origin, source_path)
                    VALUES (?, 'Nombre', 'TEXT', 'WORK', 'name')
                    """, key));
        }
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO reference_fields (key, visible_name, data_type, origin, source_path)
                VALUES ('FREE_PATH', 'Nombre', 'TEXT', 'WORK', 'client.password')
                """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO reference_fields (key, visible_name, data_type, origin, source_path)
                VALUES ('WRONG_TYPE', 'Nombre', 'DECIMAL', 'WORK', 'name')
                """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                INSERT INTO reference_fields (key, visible_name, data_type, origin, source_path)
                VALUES ('WRONG_ORIGIN', 'Nombre', 'TEXT', 'CONTRACT', 'name')
                """));
    }

    @Test
    void shouldRejectBlankAndTooLongVisibleNamesInDatabase() {
        for (String name : new String[] {"", "  ", "\t\n", "A".repeat(51)}) {
            String key = "K_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
            assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
                    INSERT INTO reference_fields (key, visible_name, data_type, origin, source_path)
                    VALUES (?, ?, 'TEXT', 'WORK', 'name')
                    """, key, name));
        }
    }

    @Test
    void shouldPreserveFirstRenameWhenSavingAnObsoleteEntity() {
        String key = "K_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        var original = register.register(key, "Anterior", ReferenceAttribute.WORK_NAME);
        var first = fields.findById(original.getId()).orElseThrow();
        var stale = fields.findById(original.getId()).orElseThrow();
        first.rename("Primero");
        fields.saveAndFlush(first);
        stale.rename("Obsoleto");
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> fields.saveAndFlush(stale));
        assertEquals("Primero", fields.findById(original.getId()).orElseThrow().getVisibleName());
    }

    @Test
    void shouldTranslateConcurrentDuplicateKeyAndKeepOneDefinition() throws Exception {
        String key = "K_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var first = concurrentSave(key, ReferenceAttribute.WORK_NAME, ready, start);
        var second = concurrentSave(key, ReferenceAttribute.CLIENT_NAME, ready, start);
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();
        var outcomes = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        assertEquals(1, outcomes.stream().filter("saved"::equals).count());
        assertEquals(1, outcomes.stream().filter("duplicate"::equals).count());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM reference_fields WHERE key = ?", Integer.class, key));
        assertThrows(DuplicateReferenceKeyException.class,
                () -> register.register(key, "Otra", ReferenceAttribute.WORK_NAME));
    }

    private CompletableFuture<String> concurrentSave(String key, ReferenceAttribute attribute,
            CountDownLatch ready, CountDownLatch start) {
        return CompletableFuture.supplyAsync(() -> {
            ready.countDown();
            try {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("No se liberó la confirmación concurrente.");
                }
                store.save(new ReferenceField(key, "Nombre", attribute));
                return "saved";
            } catch (DuplicateReferenceKeyException exception) {
                return "duplicate";
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        });
    }

    private Work createWork(String name, String clientName) {
        var client = clients.saveAndFlush(new Client(clientName, "Representante"));
        return works.saveAndFlush(new Work(client, name, "Mérida", null,
                LocalDate.of(2026, 1, 1), null));
    }

    private List<String> texts(List<ReferenceValue> values) {
        return values.stream().map(ReferenceValue::textValue).toList();
    }

    private List<String> businessSnapshot(Work work) {
        return List.of(
                jdbc.queryForObject("SELECT row_to_json(w)::text FROM works w WHERE id_work = ?", String.class, work.getId()),
                jdbc.queryForObject("SELECT row_to_json(c)::text FROM cliente c WHERE id_cliente = ?", String.class, work.getClient().getId()),
                jdbc.queryForObject("SELECT row_to_json(c)::text FROM contracts c WHERE work_id = ?", String.class, work.getId()));
    }
}
