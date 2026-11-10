package com.brica.documents.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceValueTest {
    private final ReferenceField field =
            new ReferenceField("OBRA", "Obra", ReferenceAttribute.WORK_NAME);

    @Test
    void shouldKeepPresentTextExactlyAsShownToFutureConsumers() {
        var value = ReferenceValue.resolveText(field, "  Rehabilitación de cancha  ");
        assertEquals(ResolutionStatus.PRESENT, value.status());
        assertEquals("  Rehabilitación de cancha  ", value.textValue());
        assertEquals("{{OBRA}}", value.marker());
        assertEquals("OBRA", value.key());
        assertEquals(ReferenceOrigin.WORK, value.origin());
        assertEquals(ReferenceDataType.TEXT, value.type());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void shouldDistinguishMissingText(String raw) {
        var value = ReferenceValue.resolveText(field, raw);
        assertEquals(ResolutionStatus.MISSING, value.status());
        assertNull(value.textValue());
    }

    @Test
    void shouldNotConvertAnIncompatibleValueToText() {
        for (Object raw : new Object[] {0, false, new Object()}) {
            var value = ReferenceValue.resolveText(field, raw);
            assertEquals(ResolutionStatus.INVALID, value.status());
            assertNull(value.textValue());
        }
        assertEquals(ResolutionStatus.PRESENT, ReferenceValue.resolveText(field, "0").status());
        assertEquals(ResolutionStatus.PRESENT, ReferenceValue.resolveText(field, "false").status());
    }
}
