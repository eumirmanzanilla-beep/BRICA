package com.brica.server.shared.validation.domain;

import java.util.LinkedHashMap;
import com.brica.documents.domain.*;
import com.brica.works.domain.Work;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NameRulesTest {
    @Test void validatesTheSameNamesAcrossBusinessEntities() {
        for (String name : new String[]{"A", "Niño_2", "Ñandu", "1_Obra", "123a"}) {
            var errors = new LinkedHashMap<String, String>(); InputRules.validName(errors, "name", name, 150);
            assertTrue(errors.isEmpty(), name);
            if (!Character.isDigit(name.charAt(0))) {
                assertDoesNotThrow(() -> new Work(
                        new com.brica.clients.domain.Client("Cliente", "Representante libre"), name, null, null, null, null));
            }
            assertDoesNotThrow(() -> new DocumentTemplate(name, "Tipo libre", TemplateState.CURRENT));
            assertDoesNotThrow(() -> new DocumentPackage().edit(null, name, java.util.List.of(), false));
        }
        for (String name : new String[]{"", "123", "123_456", "_Obra", "Obra uno", "Obrá", "á", "A-B", "a.b", "A\n", "Ａ", "A😀"}) {
            var errors = new LinkedHashMap<String, String>(); InputRules.validName(errors, "name", name, 150);
            assertTrue(errors.containsKey("name"), name);
            assertThrows(InvalidDataException.class, () -> new DocumentTemplate(name, "ACTA", TemplateState.CURRENT));
            assertThrows(InvalidDataException.class, () -> new DocumentPackage().edit(null, name, java.util.List.of(), false));
        }
    }
}
