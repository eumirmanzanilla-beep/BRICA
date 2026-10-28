package com.brica.clients.application;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(Long clientId) {
        super("Cliente no encontrado con id: " + clientId);
    }
}
