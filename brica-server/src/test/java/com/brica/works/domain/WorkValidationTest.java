package com.brica.works.domain;

import com.brica.clients.domain.Client;
import com.brica.server.shared.validation.domain.InvalidDataException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkValidationTest {
    @Test
    void shouldValidateOutsideHttpAndLeavePreviousValuesOnFailedCorrection() {
        var client = new Client("Cliente", "Representante");
        var start = LocalDate.of(2026, 1, 1);
        assertThrows(InvalidDataException.class, () -> new Work(client, " ", "Ubicación", null, start, null));
        assertThrows(InvalidDataException.class, () -> new Work(client, "Obra", "Ubicación", null, start, start.minusDays(1)));
        var work = new Work(client, "Obra", "Ubicación", "Texto", start, start);
        assertThrows(InvalidDataException.class, () -> work.correctGeneralInformation("", "Otra", "Otro"));
        assertEquals("Obra", work.getName());
        assertEquals("Ubicación", work.getLocation());
        assertEquals("Texto", work.getDescription());
    }
}
