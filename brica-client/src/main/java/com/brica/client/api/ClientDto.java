package com.brica.client.api;

public record ClientDto(
        Long id,
        String name,
        String legalRepresentative
) {
}
