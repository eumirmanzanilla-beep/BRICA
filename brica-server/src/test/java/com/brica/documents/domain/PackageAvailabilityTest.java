package com.brica.documents.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class PackageAvailabilityTest {
    @Test
    void emptyPackageIsImmediatelyAvailableAndCanBeRenamedAndDeleted() {
        var documentPackage = new DocumentPackage();
        documentPackage.edit(1L, "Paquete", List.of(), false);
        assertEquals(DocumentPackage.State.CONFIRMED, documentPackage.getState());
        assertTrue(documentPackage.getTemplateIds().isEmpty());
        documentPackage.edit(1L, "Nuevo_nombre", List.of(), false);
        assertEquals("Nuevo_nombre", documentPackage.getName());
        documentPackage.delete();
        assertNotNull(documentPackage.getDeletedAt());
    }

    @Test
    void versionAvailabilityCanBeChangedWithoutDeletingItsHistory() {
        var version = new TemplateVersion(1L, 1, 1L, true, "{}");
        assertTrue(version.isCurrent());
        version.setCurrent(false);
        assertFalse(version.isCurrent());
        assertNull(version.getDeletedAt());
        version.setCurrent(true);
        version.delete();
        assertThrows(DocumentRuleException.class, () -> version.setCurrent(true));
    }
}
