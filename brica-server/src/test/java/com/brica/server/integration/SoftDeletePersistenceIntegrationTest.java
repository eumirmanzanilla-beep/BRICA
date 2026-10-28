
package com.brica.server.integration;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;
import com.brica.clients.infrastructure.ClientRepository;
import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;
import com.brica.works.infrastructure.WorkRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class SoftDeletePersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private WorkRepository workRepository;

    @Autowired
    private ClientStore clientStore;

    @Autowired
    private WorkStore workStore;

    @Test
    void shouldHideDeletedEntitiesAndPreserveRelationships() {

        Client client = clientRepository.saveAndFlush(
                new Client("Cliente integración", "Representante")
        );

        Work work = workRepository.saveAndFlush(
                new Work(
                        client,
                        "Obra integración",
                        "Mérida",
                        "Prueba de eliminación lógica",
                        LocalDate.of(2026, 11, 1),
                        null
                )
        );

        Long clientId = client.getId();
        Long workId = work.getId();

        OffsetDateTime now =
                OffsetDateTime.now(ZoneOffset.UTC);

        client.markDeleted(now);
        clientRepository.saveAndFlush(client);

        // El Cliente desaparece de las consultas operativas.
        assertTrue(clientStore.findById(clientId).isEmpty());

        // La Obra se conserva, aunque su Cliente haya sido
        // eliminado lógicamente.
        assertTrue(workStore.findById(workId).isPresent());

        work.markDeleted(now);
        workRepository.saveAndFlush(work);

        // La Obra también desaparece de consultas operativas.
        assertTrue(workStore.findById(workId).isEmpty());

        // Ambos registros siguen existiendo físicamente.
        assertTrue(clientRepository.findById(clientId).isPresent());
        assertTrue(workRepository.findById(workId).isPresent());

        // La relación histórica sigue intacta.
        Work persistedWork =
                workRepository.findById(workId).orElseThrow();

        assertEquals(clientId, persistedWork.getClient().getId());
        assertNotNull(persistedWork.getDeletedAt());
    }
}
