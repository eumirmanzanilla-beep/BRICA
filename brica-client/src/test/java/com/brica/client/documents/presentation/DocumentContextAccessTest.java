package com.brica.client.documents.presentation;

import com.brica.client.core.http.ServerSession.SessionUser;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentContextAccessTest {
    @Test
    void requiresBothWorkReadAndDocumentConfigurationPermissions() {
        assertFalse(DocumentContextAccess.canConfigureForWork(null));
        assertFalse(DocumentContextAccess.canConfigureForWork(user("WORKS_READ")));
        assertFalse(DocumentContextAccess.canConfigureForWork(user("DOCUMENT_CONFIG_ADMIN")));
        assertTrue(DocumentContextAccess.canConfigureForWork(user("WORKS_READ", "DOCUMENT_CONFIG_ADMIN")));
    }

    private static SessionUser user(String... permissions) {
        return new SessionUser(1L, "test", "Test", 1L, "Test", true, Set.of(permissions));
    }
}
