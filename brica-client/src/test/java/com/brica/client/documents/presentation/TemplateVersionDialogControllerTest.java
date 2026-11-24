package com.brica.client.documents.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateVersionDialogControllerTest {
    @Test
    void suggestsTheDocxBaseNameWithoutChangingOtherNames() {
        assertEquals("Acta Inicio 01", TemplateVersionDialogController.suggestedVersionName("Acta Inicio 01.DOCX"));
        assertEquals("plantilla", TemplateVersionDialogController.suggestedVersionName("plantilla"));
        assertEquals("", TemplateVersionDialogController.suggestedVersionName(""));
    }
}
