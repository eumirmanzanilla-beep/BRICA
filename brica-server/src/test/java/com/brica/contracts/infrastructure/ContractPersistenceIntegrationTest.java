package com.brica.contracts.infrastructure;

import com.brica.clients.domain.Client;
import com.brica.clients.infrastructure.ClientRepository;
import com.brica.contracts.domain.Contract;
import com.brica.works.domain.Work;
import com.brica.works.infrastructure.WorkRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest
class ContractPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("brica_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private WorkRepository workRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void shouldPersistJsonbAndWorkRelationship() {
        // Arrange: crear un cliente y una obra reales en PostgreSQL.
        Client client = clientRepository.saveAndFlush(
                new Client(
                        "Cliente_integracion",
                        "Representante de integración"
                )
        );

        Work work = workRepository.saveAndFlush(
                new Work(
                        client,
                        "Obra_integracion",
                        "Mérida, Yucatán",
                        "Obra creada para probar la persistencia del contrato",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31)
                )
        );

        Map<String, Object> contractData = Map.of(
                "contractNumber", "CTR-INT-001",
                "requiresReview", true,
                "metadata", Map.of(
                        "source", "testcontainers",
                        "classification", "integration-test"
                )
        );

        // Act: guardar el contrato utilizando el repositorio real.
        Contract savedContract = contractRepository.saveAndFlush(
                new Contract(work, contractData)
        );

        Long contractId = savedContract.getId();

        assertNotNull(contractId);

        // Vaciar el contexto para obligar a recuperar los datos de la BD.
        entityManager.clear();

        Contract reloadedContract = contractRepository.findById(contractId)
                .orElseThrow();

        // Assert: se conservó la relación con la obra.
        assertEquals(
                work.getId(),
                reloadedContract.getWork().getId()
        );

        // Assert: JSONB se recuperó con sus valores y estructura.
        assertEquals(
                "CTR-INT-001",
                reloadedContract.getData().get("contractNumber")
        );

        assertEquals(
                Boolean.TRUE,
                reloadedContract.getData().get("requiresReview")
        );

        assertEquals(
                Map.of(
                        "source", "testcontainers",
                        "classification", "integration-test"
                ),
                reloadedContract.getData().get("metadata")
        );

        // Assert: PostgreSQL almacena la columna con tipo JSONB.
        String columnType = jdbcTemplate.queryForObject(
                """
                SELECT data_type
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name = 'contracts'
                  AND column_name = 'data'
                """,
                String.class
        );

        assertEquals("jsonb", columnType);

        // Assert: el JSON y la FK también existen correctamente en SQL.
        String storedContractNumber = jdbcTemplate.queryForObject(
                "SELECT data ->> 'contractNumber' FROM contracts WHERE id_contract = ?",
                String.class,
                contractId
        );

        Long storedWorkId = jdbcTemplate.queryForObject(
                "SELECT work_id FROM contracts WHERE id_contract = ?",
                Long.class,
                contractId
        );

        assertEquals("CTR-INT-001", storedContractNumber);
        assertEquals(work.getId(), storedWorkId);
    }

    @Test
    @Transactional
    void shouldRejectContractWithNonexistentWork() {
        // Una obra inexistente debe ser rechazada por la FK de PostgreSQL.
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO contracts (work_id, data)
                        VALUES (?, CAST(? AS jsonb))
                        """,
                        -999999L,
                        """
                        {"contractNumber":"CTR-INVALID"}
                        """
                )
        );
    }
}
