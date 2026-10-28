package com.brica.clients.infrastructure;

import java.util.List;
import java.util.Optional;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;

import org.springframework.stereotype.Repository;

@Repository
public class JpaClientStore implements ClientStore {

    private final ClientRepository clientRepository;

    public JpaClientStore(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Client save(Client client) {
        return clientRepository.save(client);
    }

    @Override
    public Optional<Client> findById(Long id) {
        return clientRepository.findById(id);
    }

    @Override
    public List<Client> list() {
        return clientRepository.findAll();
    }
}
