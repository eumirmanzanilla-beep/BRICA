package com.brica.documents.application;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.application.port.WorkReferenceSource;
import com.brica.documents.domain.ReferenceAttribute;
import com.brica.documents.domain.ReferenceField;
import com.brica.documents.domain.ResolutionStatus;
import com.brica.server.shared.validation.domain.InvalidDataException;
import com.brica.works.application.WorkNotFoundException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResolveReferenceValuesServiceTest {
    private final ReferenceFieldStore fields = mock(ReferenceFieldStore.class);
    private final WorkReferenceSource source = mock(WorkReferenceSource.class);
    private final ResolveReferenceValuesService service = new ResolveReferenceValuesService(fields, source);

    @Test
    void shouldResolveOnlyRequestedFieldsUsingTheSelectedWorkAndItsClient() {
        var work = field(1L, "OBRA", ReferenceAttribute.WORK_NAME);
        var client = field(2L, "CLIENTE", ReferenceAttribute.CLIENT_NAME);
        when(fields.findAllById(any())).thenReturn(List.of(client, work));
        when(source.findByWorkId(10L)).thenReturn(Optional.of(new WorkReferenceData("Obra A", "Cliente A")));
        when(source.findByWorkId(20L)).thenReturn(Optional.of(new WorkReferenceData("Obra B", "Cliente B")));
        var first = service.resolveForWork(10L, List.of(1L, 2L, 1L));
        var second = service.resolveForWork(20L, List.of(1L, 2L));
        assertEquals(List.of("Obra A", "Cliente A"), first.stream().map(v -> v.textValue()).toList());
        assertEquals(List.of("Obra B", "Cliente B"), second.stream().map(v -> v.textValue()).toList());
        assertTrue(first.stream().allMatch(v -> v.status() == ResolutionStatus.PRESENT));
        verify(fields, never()).save(any());
    }

    @Test
    void shouldRejectMissingOrDeletedWorkContext() {
        when(source.findByWorkId(1L)).thenReturn(Optional.empty());
        assertThrows(WorkNotFoundException.class, () -> service.resolveForWork(1L, List.of(1L)));
        verifyNoInteractions(fields);
    }

    @Test
    void shouldReportUnknownFieldInsteadOfSilentlySkippingIt() {
        when(source.findByWorkId(1L)).thenReturn(Optional.of(new WorkReferenceData("Obra", "Cliente")));
        when(fields.findAllById(any())).thenReturn(List.of());
        assertThrows(ReferenceFieldNotFoundException.class, () -> service.resolveForWork(1L, List.of(99L)));
    }

    @Test
    void shouldPropagateReadFailureRatherThanReturnMissingValues() {
        var failure = new ReferenceSourceException(new IllegalStateException("storage failure"));
        when(source.findByWorkId(1L)).thenThrow(failure);
        assertSame(failure, assertThrows(ReferenceSourceException.class,
                () -> service.resolveForWork(1L, List.of(1L))));
        verifyNoInteractions(fields);
    }

    @Test
    void shouldValidateAllIdentifiersBeforeQuerying() {
        assertThrows(InvalidDataException.class, () -> service.resolveForWork(null, List.of(1L)));
        assertThrows(InvalidDataException.class, () -> service.resolveForWork(1L, null));
        assertThrows(InvalidDataException.class, () -> service.resolveForWork(1L, List.of(0L)));
        assertThrows(InvalidDataException.class, () -> service.resolveForWork(1L, Arrays.asList(1L, null)));
        verifyNoInteractions(fields, source);
    }

    private ReferenceField field(Long id, String key, ReferenceAttribute attribute) {
        var field = spy(new ReferenceField(key, key, attribute));
        when(field.getId()).thenReturn(id);
        return field;
    }
}
