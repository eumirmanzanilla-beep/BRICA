package com.brica.client.clients.presentation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ClientNameInputRulesTest {
    @Test
    void acceptsLegibleNamesAndRejectsPunctuationOrInvalidStarts() {
        assertTrue(ClientNameInputRules.valid("Ñandú Álamo - Norte_2", 255));
        assertTrue(ClientNameInputRules.valid("Cliente 2", 255));

        for (String invalid : new String[]{"", "   ", "123", " Cliente", "2 Cliente", "_Cliente", "-Cliente",
                "Cliente.SA", "Cliente, SA", "Cliente; SA"}) {
            assertFalse(ClientNameInputRules.valid(invalid, 255), invalid);
        }
    }
}
