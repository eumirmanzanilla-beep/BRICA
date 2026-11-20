package com.brica.clients.api;

import com.brica.clients.domain.Client;

public record ClientResponse(
        Long id,
        String name,
        String legalRepresentative,
        String documentName,
        String revision
) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(
            client.getId(),
            client.getName(),
            client.getLegalRepresentative(),
            client.getDocumentName(),
            com.brica.clients.application.ClientRevision.etag(client)
        );
    }
}
