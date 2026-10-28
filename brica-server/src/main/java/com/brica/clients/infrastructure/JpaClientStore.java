package com.brica.clients.infrastructure;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;
import org.springframework.stereotype.Repository;

@Repository
public class JpaClientStore implements ClientStore {
    private final ClientRepository repository;

    public JpaClientStore(ClientRepository repository) { this.repository = repository; }

    @Override
    public Client save(Client entity) { return repository.save(entity); }
    @Override
    public java.util.Optional<Client> findById(Long id) { return repository.findById(id); }

    @Override
    public java.util.List<Client> list(int page, int size) {
        return repository.findAll(org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("id"))).getContent();
    }
}
