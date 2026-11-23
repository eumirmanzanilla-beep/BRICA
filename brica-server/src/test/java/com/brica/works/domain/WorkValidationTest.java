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
        assertThrows(InvalidDataException.class, () -> new Work(client, null, "Ubicación", null, start, null));
        assertThrows(InvalidDataException.class, () -> new Work(client, " ", "Ubicación", null, start, null));
        assertThrows(InvalidDataException.class, () -> new Work(client, "a".repeat(256), "Ubicación", null, start, null));
        assertThrows(InvalidDataException.class, () -> new Work(client, "Obra", "Ubicación", null, start, start.minusDays(1)));
        var work = new Work(client, "Edificio 2: Ampliación, Mérida", "Ubicación", "Texto", start, start);
        assertThrows(InvalidDataException.class, () -> work.correctGeneralInformation("", "Otra", "Otro"));
        assertEquals("Edificio 2: Ampliación, Mérida", work.getName());
        assertEquals("Ubicación", work.getLocation());
        assertEquals("Texto", work.getDescription());
    }

    @Test
    void shouldAcceptPresentationNamesWithSpacesAccentsNumbersAndPunctuation() {
        var work = new Work(
                new Client("Cliente", "Representante"),
                "Torre 3 - Etapa 2 (Niños)",
                null,
                null,
                null,
                null
        );

        work.correctGeneralInformation("Ampliación área común #2", null, null);

        assertEquals("Ampliación área común #2", work.getName());
    }
}
