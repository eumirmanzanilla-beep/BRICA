package com.brica.client.works.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class WorkNameInputRulesTest {
    @Test
    void acceptsOnlyNamesThatStartWithALetterAndUseIdentifierCharacters() {
        assertEquals("", WorkNameInputRules.error("Obra_2"));
        assertEquals("", WorkNameInputRules.error("Álamo - Ñ2"));
        assertEquals("", WorkNameInputRules.error("Obra 2"));

        assertEquals("Usa letras, números, espacios y guiones; el nombre debe iniciar con una letra.", WorkNameInputRules.error("2Obra"));
        assertEquals("Usa letras, números, espacios y guiones; el nombre debe iniciar con una letra.", WorkNameInputRules.error("_Obra"));
        assertEquals("Usa letras, números, espacios y guiones; el nombre debe iniciar con una letra.", WorkNameInputRules.error("-Obra"));
        assertEquals("Usa letras, números, espacios y guiones; el nombre debe iniciar con una letra.", WorkNameInputRules.error(" Obra"));
    }
}
