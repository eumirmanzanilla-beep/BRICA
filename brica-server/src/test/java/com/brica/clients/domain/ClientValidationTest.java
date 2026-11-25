package com.brica.clients.domain;

import com.brica.server.shared.validation.domain.InvalidDataException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientValidationTest {
    @Test
    void shouldValidateRequiredFieldsAndLengthsOutsideHttp() {
        var exception = assertThrows(InvalidDataException.class, () -> new Client(" ", null));
        assertTrue(exception.getErrors().containsKey("name"));
        assertTrue(exception.getErrors().containsKey("legalRepresentative"));
        assertThrows(InvalidDataException.class, () -> new Client("a".repeat(256), "Representante"));
    }

    @Test
    void shouldAcceptLegibleClientNamesAndRejectOtherPunctuationOrInvalidStarts() {
        assertDoesNotThrow(() -> new Client("Constructora Álamo - Norte_2", "Representante"));
        for (String invalid : new String[]{"2 Constructora", "_Constructora", "-Constructora",
                "Constructora.SA", "Constructora, SA", "Constructora; SA"}) {
            assertThrows(InvalidDataException.class, () -> new Client(invalid, "Representante"), invalid);
        }
    }
}
