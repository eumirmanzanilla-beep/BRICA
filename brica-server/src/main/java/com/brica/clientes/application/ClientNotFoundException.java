package com.brica.clientes.application;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(Long clientId) {
        super("Client not found with id: " + clientId);
    }
}