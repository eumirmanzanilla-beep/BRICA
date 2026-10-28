package com.brica.clients.application.port;

import com.brica.clients.domain.Client;

public interface ClientStore {
    Client save(Client entity);
    java.util.Optional<Client> findById(Long id);
    java.util.List<Client> list(int page, int size);
}
