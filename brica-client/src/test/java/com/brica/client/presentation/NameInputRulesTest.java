package com.brica.client.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NameInputRulesTest {
    @Test void matchesServerRulesWithoutTrimmingOrNormalizing() {
        for (String name : new String[]{"A", "Niño_2", "Ñandu", "1_Obra", "123a"}) { assertTrue(NameInputRules.valid(name, 150), name); }
        for (String name : new String[]{"", "123", "123_456", "_Obra", "Obra uno", "Obrá", "á", "A-B", "a.b", "A\n", "Ａ", "A😀"}) {
            assertFalse(NameInputRules.valid(name, 150), name);
            assertThrows(IllegalArgumentException.class, () -> NameInputRules.require(name, 150));
        }
        assertFalse(NameInputRules.valid("A".repeat(151), 150));
    }
}
