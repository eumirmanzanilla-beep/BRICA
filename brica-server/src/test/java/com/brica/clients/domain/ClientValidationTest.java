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
}
