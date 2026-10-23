package com.brica.clientes.api;

import com.brica.clientes.domain.Cliente;

public record ClientResponse(
        Long id,
        String name,
        String legalRepresentative
) {

    public static ClientResponse from(Cliente client) {
        return new ClientResponse(
            client.getIdCliente(),
            client.getNombre(),
            client.getRepresentanteLegal()
        );
    }
}