package com.brica.client.documents.presentation;

import com.brica.client.core.http.ServerSession.SessionUser;

/** Client-side affordance only; the server remains authoritative for every document operation. */
public final class DocumentContextAccess {
    private DocumentContextAccess() { }

    public static boolean canConfigureForWork(SessionUser user) {
        return user != null
                && user.can("WORKS_READ")
                && user.can("DOCUMENT_CONFIG_ADMIN");
    }
}
