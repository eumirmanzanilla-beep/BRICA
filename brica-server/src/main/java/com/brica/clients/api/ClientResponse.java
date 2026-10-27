package com.brica.clients.api;

import com.brica.clients.domain.Client;

public record ClientResponse(
        Long id,
        String name,
        String legalRepresentative
) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(
            client.getId(),
            client.getName(),
            client.getLegalRepresentative()
        );
    }
}