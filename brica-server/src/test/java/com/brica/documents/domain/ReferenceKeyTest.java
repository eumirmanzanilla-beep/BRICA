package com.brica.documents.domain;

import com.brica.server.shared.validation.domain.InvalidDataException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceKeyTest {
    @ParameterizedTest
    @ValueSource(strings = {"NOMBRE_OBRA", "A", "0", "_", "123_ABC"})
    void shouldRoundTripCanonicalKeys(String key) {
        var reference = new ReferenceKey(key);
        assertEquals("{{" + key + "}}", reference.marker());
        assertEquals(reference, ReferenceKey.fromMarker(reference.marker()));
    }

    @Test
    void shouldAllowFiftyCharacterKeyAndFiftyFourCharacterMarker() {
        var key = new ReferenceKey("A".repeat(50));
        assertEquals(54, key.marker().length());
        assertEquals(key, ReferenceKey.fromMarker(key.marker()));
        assertThrows(InvalidDataException.class, () -> new ReferenceKey("A".repeat(51)));
        assertThrows(InvalidDataException.class,
                () -> ReferenceKey.fromMarker("{{" + "A".repeat(51) + "}}"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"nombre", "Nombre_Obra", " NOMBRE", "NOMBRE ", "NOMBRE OBRA",
            "OBRA-1", "OBRA.1", "OBRA\n", "ÁREA", "ＮＯＭＢＲＥ", "{{NOMBRE}}", "\t"})
    void shouldRejectNoncanonicalKeysWithoutNormalizingThem(String value) {
        var error = assertThrows(InvalidDataException.class, () -> new ReferenceKey(value));
        assertTrue(error.getErrors().containsKey("key"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"{{}}", "{{nombre}}", "{{ NOMBRE }}", "{NOMBRE}", "{{{NOMBRE}}}",
            " {{NOMBRE}}", "{{NOMBRE}} ", "{{NOMBRE}}\n", "{{NOMBRE", "NOMBRE}}", "{{A}}{{B}}"})
    void shouldRequireAnExactCompleteMarker(String marker) {
        var error = assertThrows(InvalidDataException.class, () -> ReferenceKey.fromMarker(marker));
        assertTrue(error.getErrors().containsKey("marker"));
    }
}
