package com.brica.documents.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DocumentTemplateTest {
    @Test
    void activatesADraftAfterItsInitialVersion() {
        var template = new DocumentTemplate("Acta", "ACTA", TemplateState.DRAFT);

        template.activateWhenVersionIsAvailable();

        assertEquals(TemplateState.CURRENT, template.getState());
    }

    @Test
    void doesNotReactivateAnUnavailableTemplate() {
        var template = new DocumentTemplate("Acta", "ACTA", TemplateState.UNAVAILABLE);

        template.activateWhenVersionIsAvailable();

        assertEquals(TemplateState.UNAVAILABLE, template.getState());
    }
}
