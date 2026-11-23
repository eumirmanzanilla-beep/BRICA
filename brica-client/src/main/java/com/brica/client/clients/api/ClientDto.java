package com.brica.client.clients.api;

public record ClientDto(
        Long id,
        String name,
        String legalRepresentative,
        String documentName,
        String revision
) {
    public ClientDto(Long id, String name, String legalRepresentative) { this(id, name, legalRepresentative, null, null); }
}
