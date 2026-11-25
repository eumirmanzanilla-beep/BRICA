package com.brica.client.documents.presentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateVersionDialogControllerTest {
    @Test
    void suggestsAValidIdentifierFromTheDocxBaseName() {
        assertEquals("Acta_Inicio_01", TemplateVersionDialogController.suggestedVersionName("Acta Inicio 01.DOCX"));
        assertEquals("plantilla", TemplateVersionDialogController.suggestedVersionName("plantilla"));
        assertEquals("Documento_2026", TemplateVersionDialogController.suggestedVersionName("2026.docx"));
        assertEquals("01_Acta_Inicio", TemplateVersionDialogController.suggestedVersionName("01_Acta_Inicio.docx"));
        assertEquals("", TemplateVersionDialogController.suggestedVersionName(""));
    }
    @Test
    void explainsTheFieldErrorAndPossibleSolution() {
        var diagnostic = new com.brica.client.documents.api.DocumentDtos.Diagnostic("UNKNOWN_MARKER",
                "word/document.xml:p4", "{{CAMPO_NO_REGISTRADO}}", "Clave no registrada: CAMPO_NO_REGISTRADO");

        assertEquals("""
                Clave: CAMPO_NO_REGISTRADO
                Marcador: {{CAMPO_NO_REGISTRADO}}
                Problema: Clave no registrada: CAMPO_NO_REGISTRADO
                Cómo corregirlo: Crea el campo con esa clave o sustitúyelo por un marcador ya registrado.
                Ubicación: word/document.xml:p4""", TemplateVersionDialogController.diagnosticDetails(diagnostic));
    }
    @Test
    void explainsHowToCorrectAnInvalidMarkerWhenItWasCollected() {
        var diagnostic = new com.brica.client.documents.api.DocumentDtos.Diagnostic("INVALID_MARKER",
                "word/header1.xml:p1", "{{nombre obra}}", "Marcador mal formado o dividido entre párrafos.");

        var details = TemplateVersionDialogController.diagnosticDetails(diagnostic);

        assertTrue(details.contains("Clave: nombre obra"));
        assertTrue(details.contains("Marcador: {{nombre obra}}"));
        assertTrue(details.contains("Problema: Marcador mal formado o dividido entre párrafos."));
        assertTrue(details.contains("Cómo corregirlo: Usa exactamente {{CLAVE}}, con 1 a 50 letras mayúsculas, números o _."));
    }
    @Test
    void explainsAStandaloneClosingBraceWithoutPretendingItHasAKey() {
        var diagnostic = new com.brica.client.documents.api.DocumentDtos.Diagnostic("INVALID_MARKER",
                "word/document.xml:p8", "}", "El carácter '}' no tiene una llave de apertura '{' correspondiente.");

        var details = TemplateVersionDialogController.diagnosticDetails(diagnostic);

        assertTrue(details.contains("Clave: No aplica"));
        assertTrue(details.contains("Marcador: }"));
        assertTrue(details.contains("Problema: El carácter '}' no tiene una llave de apertura '{' correspondiente."));
    }
}
