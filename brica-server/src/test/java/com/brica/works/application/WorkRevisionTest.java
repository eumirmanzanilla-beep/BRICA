package com.brica.works.application;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import com.brica.clients.domain.Client;
import com.brica.works.domain.Work;
import com.brica.works.domain.WorkStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkRevisionTest {
    @Test void additionalValuesAreOrderedAndUnambiguous() {
        var work = new Work(new Client("Cliente", "Representante"), "Obra", "Ubicación", null,
                LocalDate.of(2026, 1, 1), null);
        var values = new LinkedHashMap<String, String>();
        values.put("A", "x"); values.put("B", "y");
        setValues(work, values);
        String revision = WorkRevision.etag(work);
        var reversed = new LinkedHashMap<String, String>();
        reversed.put("B", "y"); reversed.put("A", "x");
        setValues(work, reversed);
        assertEquals(revision, WorkRevision.etag(work));
        setValues(work, Map.of("A", "x, B=y"));
        assertNotEquals(revision, WorkRevision.etag(work));
    }

    private void setValues(Work work, Map<String, String> values) {
        work.updateDocumentInformation(null, null, null, null, null, LocalDate.of(2026, 1, 1), null,
                null, WorkStatus.IN_PROGRESS, values);
    }
}
