package com.brica.clients.application;

import com.brica.clients.domain.Client;
import com.brica.documents.application.DocumentHashes;

public final class ClientRevision {
    private ClientRevision() { }
    public static String etag(Client client) {
        var text = new StringBuilder();
        for (Object value : new Object[]{client.getId(), client.getName(), client.getLegalRepresentative(),
                client.getDocumentName(), client.getDeletedAt(), client.getRevision()}) {
            String part = value == null ? "N" : "V" + value;
            text.append(part.length()).append(':').append(part);
        }
        return "\"" + DocumentHashes.sha256(text.toString()) + "\"";
    }
    public static void check(Client client, String expected) {
        if (expected == null || expected.isBlank()) { throw new com.brica.works.application.MissingPreconditionException(); }
        if (!etag(client).equals(expected)) { throw new com.brica.documents.application.DocumentException(412,
                "STALE_CLIENT", "El cliente cambió; vuelve a consultar antes de guardar."); }
    }
}
