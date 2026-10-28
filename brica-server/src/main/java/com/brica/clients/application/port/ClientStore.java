package com.brica.clients.application.port;

import java.util.List;
import java.util.Optional;

import com.brica.clients.domain.Client;

public interface ClientStore {

    Client save(Client client);

    Optional<Client> findById(Long id);

    List<Client> list();
}
