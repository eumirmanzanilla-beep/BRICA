package com.brica.client.presentation;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocumentInputTest {
    @Test void acceptsSpanishAndIsoDatesWithoutLenientNormalization() {
        var date = LocalDate.of(2005, 7, 16);
        assertEquals(date, DocumentInput.date("16/07/2005"));
        assertEquals(date, DocumentInput.date("2005-07-16"));
        assertEquals("16/07/2005", DocumentInput.format(date));
        assertNull(DocumentInput.date(" "));
        assertThrows(java.time.DateTimeException.class, () -> DocumentInput.date("31/02/2026"));
    }
}
