package com.brica.documents.domain;

import com.brica.server.shared.validation.domain.InvalidDataException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceFieldTest {
    @Test
    void shouldDeriveDefinitionFromApprovedAttribute() {
        var work = new ReferenceField("NOMBRE_OBRA", "Obra", ReferenceAttribute.WORK_NAME);
        var client = new ReferenceField("CLIENTE", "Cliente", ReferenceAttribute.CLIENT_NAME);
        assertEquals(ReferenceDataType.TEXT, work.getType());
        assertEquals(ReferenceOrigin.WORK, work.getOrigin());
        assertEquals(ReferenceOrigin.CLIENT, client.getOrigin());
        assertEquals("name", work.getSourcePath());
        assertEquals("name", client.getSourcePath());
        assertEquals(ReferenceAttribute.CLIENT_NAME, client.getAttribute());
    }

    @Test
    void shouldOnlyChangeVisibleNameWhenRenaming() {
        var field = new ReferenceField("NOMBRE_OBRA", "Anterior", ReferenceAttribute.WORK_NAME);
        field.rename("Nuevo nombre");
        assertEquals("Nuevo nombre", field.getVisibleName());
        assertEquals("NOMBRE_OBRA", field.getKey());
        assertEquals(ReferenceAttribute.WORK_NAME, field.getAttribute());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    void shouldRejectBlankNameWithoutChangingDefinition(String name) {
        var field = new ReferenceField("NOMBRE_OBRA", "Anterior", ReferenceAttribute.WORK_NAME);
        assertThrows(InvalidDataException.class, () -> field.rename(name));
        assertEquals("Anterior", field.getVisibleName());
        assertEquals(ReferenceAttribute.WORK_NAME, field.getAttribute());
    }

    @Test
    void shouldEnforceDictionaryVisibleNameLimit() {
        var field = new ReferenceField("NOMBRE", "A".repeat(50), ReferenceAttribute.WORK_NAME);
        assertThrows(InvalidDataException.class, () -> field.rename("A".repeat(51)));
        assertEquals("A".repeat(50), field.getVisibleName());
    }

    @Test
    void shouldRejectMissingAttributeAndFreeSourcePaths() {
        assertThrows(InvalidDataException.class, () -> new ReferenceField("NOMBRE", "Obra", null));
        assertThrows(InvalidDataException.class, () -> ReferenceAttribute.fromDefinition(
                ReferenceDataType.TEXT, ReferenceOrigin.WORK, "client.password"));
        assertThrows(InvalidDataException.class, () -> ReferenceAttribute.fromDefinition(
                null, ReferenceOrigin.WORK, "name"));
    }
}
