package com.brica.client.documents.presentation;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class TemplateVersionDialogTest {
    @Test
    void resolvesTheVersionDialogResourceOutsideThePresentationPackage() {
        assertNotNull(TemplateVersionDialog.loader().getLocation());
    }
}
