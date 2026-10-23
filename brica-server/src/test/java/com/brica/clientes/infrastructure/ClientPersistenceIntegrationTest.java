package com.brica.clientes.infrastructure;

import com.brica.clientes.domain.Cliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
@Transactional
class ClientPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    private ClienteRepository clientRepository;

    @Test
    void shouldPersistClient() {
        Cliente client = new Cliente(
            "Constructora Integration Test",
            "Jane Doe"
        );

        Cliente savedClient =
            clientRepository.saveAndFlush(client);

        assertNotNull(savedClient.getIdCliente());

        Cliente persistedClient =
            clientRepository
                .findById(savedClient.getIdCliente())
                .orElseThrow();

        assertEquals(
            "Constructora Integration Test",
            persistedClient.getNombre()
        );

        assertEquals(
            "Jane Doe",
            persistedClient.getRepresentanteLegal()
        );
    }
}