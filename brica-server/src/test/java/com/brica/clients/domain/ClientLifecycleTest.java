
package com.brica.clients.domain;

import java.time.OffsetDateTime;

import com.brica.server.shared.validation.domain.InvalidDataException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientLifecycleTest {

    @Test
    void shouldUpdateValidClientData() {

        Client client = new Client("Original", "Representante A");

        client.updateDetails("Actualizado", "Representante B");

        assertEquals("Actualizado", client.getName());
        assertEquals("Representante B", client.getLegalRepresentative());
    }

    @Test
    void shouldPreserveOriginalValuesWhenUpdateFails() {

        Client client = new Client("Original", "Representante A");

        assertThrows(
                InvalidDataException.class,
                () -> client.updateDetails("", "Representante B")
        );

        assertEquals("Original", client.getName());
        assertEquals("Representante A", client.getLegalRepresentative());
    }

    @Test
    void shouldMarkClientAsDeleted() {

        Client client = new Client("Original", "Representante A");

        OffsetDateTime deletionDate =
                OffsetDateTime.parse("2026-10-28T16:00:00Z");

        client.markDeleted(deletionDate);

        assertEquals(deletionDate, client.getDeletedAt());
    }
}
