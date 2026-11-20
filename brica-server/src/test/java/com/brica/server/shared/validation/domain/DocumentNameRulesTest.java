package com.brica.server.shared.validation.domain;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocumentNameRulesTest {
    @Test void preservesLiteralDocumentNameSeparatelyFromRestrictedTechnicalNames() {
        assertEquals("Construcción de fachada, 2.ª etapa — Ñ",DocumentNameRules.normalize("Construcción de fachada, 2.ª etapa — Ñ"));
        assertNull(DocumentNameRules.normalize("  ")); assertNull(DocumentNameRules.normalize(null));
        assertThrows(InvalidDataException.class,()->DocumentNameRules.normalize("Nombre\nOtro"));
        assertThrows(InvalidDataException.class,()->DocumentNameRules.normalize("\n"));
        assertThrows(InvalidDataException.class,()->DocumentNameRules.normalize("x".repeat(1001)));
    }
}
