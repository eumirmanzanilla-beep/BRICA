package com.brica.documents.application;

import java.util.List;
import java.util.Optional;

import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.domain.ReferenceAttribute;
import com.brica.documents.domain.ReferenceField;
import com.brica.server.shared.validation.domain.InvalidDataException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReferenceFieldServicesTest {
    private final ReferenceFieldStore store = mock(ReferenceFieldStore.class);

    @Test
    void shouldValidateBeforeAccessingPersistence() {
        var service = new RegisterReferenceFieldService(store);
        assertThrows(InvalidDataException.class,
                () -> service.register("nombre", "Obra", ReferenceAttribute.WORK_NAME));
        verifyNoInteractions(store);
    }

    @Test
    void shouldRejectDuplicateKeyBeforeSaving() {
        when(store.existsByKey("OBRA")).thenReturn(true);
        assertThrows(DuplicateReferenceKeyException.class,
                () -> new RegisterReferenceFieldService(store)
                        .register("OBRA", "Obra", ReferenceAttribute.WORK_NAME));
        verify(store, never()).save(any());
    }

    @Test
    void shouldRegisterAnApprovedDefinition() {
        when(store.save(any())).thenAnswer(call -> call.getArgument(0));
        var field = new RegisterReferenceFieldService(store)
                .register("CLIENTE", "Cliente", ReferenceAttribute.CLIENT_NAME);
        assertEquals(ReferenceAttribute.CLIENT_NAME, field.getAttribute());
        verify(store).save(field);
    }

    @Test
    void shouldRenameWithoutReassigningAttribute() {
        var field = new ReferenceField("OBRA", "Anterior", ReferenceAttribute.WORK_NAME);
        when(store.findById(1L)).thenReturn(Optional.of(field));
        when(store.save(field)).thenReturn(field);
        assertSame(field, new UpdateReferenceFieldService(store).rename(1L, "Actual"));
        assertEquals("Actual", field.getVisibleName());
        assertEquals(ReferenceAttribute.WORK_NAME, field.getAttribute());
    }

    @Test
    void shouldRejectMissingFieldAndInvalidIdentifier() {
        var service = new UpdateReferenceFieldService(store);
        assertThrows(InvalidDataException.class, () -> service.rename(0L, "Nombre"));
        verifyNoInteractions(store);
        when(store.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ReferenceFieldNotFoundException.class, () -> service.rename(1L, "Nombre"));
        verify(store, never()).save(any());
    }

    @Test
    void shouldDeleteOnlyAnExistingField() {
        var service = new UpdateReferenceFieldService(store);
        assertThrows(InvalidDataException.class, () -> service.delete(0L));
        assertThrows(ReferenceFieldNotFoundException.class, () -> service.delete(1L));
        when(store.findById(1L)).thenReturn(Optional.of(new ReferenceField("OBRA", "Obra", ReferenceAttribute.WORK_NAME)));
        service.delete(1L);
        verify(store).delete(1L);
    }

    @Test
    void shouldListOnlyApprovedAttributesAndValidatePagination() {
        var service = new ListReferenceFieldsService(store);
        assertEquals(List.of(ReferenceAttribute.values()), service.listApprovedAttributes());
        assertThrows(InvalidDataException.class, () -> service.list(-1, 10));
        assertThrows(InvalidDataException.class, () -> service.list(0, 0));
        assertThrows(InvalidDataException.class, () -> service.list(0, 201));
        verifyNoInteractions(store);
        when(store.list(0, 50)).thenReturn(List.of());
        assertEquals(List.of(), service.list(0, 50));
    }
}
