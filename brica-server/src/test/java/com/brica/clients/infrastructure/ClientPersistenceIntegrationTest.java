package com.brica.clients.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.brica.clients.domain.Client;
import com.brica.clients.infrastructure.ClientRepository;

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
    private ClientRepository clientRepository;

    @Test
    void shouldPersistClient() {
        Client client = new Client(
            "Constructora Integration Test",
            "Jane Doe"
        );

        Client savedClient =
            clientRepository.saveAndFlush(client);

        assertNotNull(savedClient.getId());

        Client persistedClient =
            clientRepository
                .findById(savedClient.getId())
                .orElseThrow();

        assertEquals(
            "Constructora Integration Test",
            persistedClient.getName()
        );

        assertEquals(
            "Jane Doe",
            persistedClient.getLegalRepresentative()
        );
    }
}