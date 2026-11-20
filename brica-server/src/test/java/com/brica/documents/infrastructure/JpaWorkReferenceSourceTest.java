package com.brica.documents.infrastructure;

import com.brica.documents.application.ReferenceSourceException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JpaWorkReferenceSourceTest {
    @Test
    void shouldTranslateReadFailuresWithoutClassifyingThemAsMissingData() {
        var repository = mock(WorkReferenceRepository.class);
        var cause = new DataAccessResourceFailureException("internal database details");
        when(repository.findReferenceData(1L)).thenThrow(cause);
        var error = assertThrows(ReferenceSourceException.class,
                () -> new JpaWorkReferenceSource(repository).findByWorkId(1L));
        assertSame(cause, error.getCause());
        assertFalse(error.getMessage().contains("internal database details"));
    }
}
