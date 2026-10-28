
package com.brica.works.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.brica.clients.domain.Client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkSoftDeleteTest {

    @Test
    void shouldMarkWorkAsDeletedWithoutChangingItsData() {

        Client client = new Client("Cliente", "Representante");

        Work work = new Work(
                client,
                "Obra original",
                "Mérida",
                "Descripción",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 12, 1)
        );

        OffsetDateTime deletionDate =
                OffsetDateTime.parse("2026-10-28T16:00:00Z");

        work.markDeleted(deletionDate);

        assertEquals(deletionDate, work.getDeletedAt());
        assertEquals("Obra original", work.getName());
        assertSame(client, work.getClient());
    }
}
