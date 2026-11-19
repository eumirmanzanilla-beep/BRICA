
package com.brica.clients.infrastructure;

import java.util.List;
import java.util.Optional;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaClientStore implements ClientStore {

    private final ClientRepository repository;

    public JpaClientStore(ClientRepository repository) {
        this.repository = repository;
    }

    @Override
    public Client save(Client client) {
        return repository.saveAndFlush(client);
    }

    @Override
    public Optional<Client> findById(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id);
    }

    @Override public Optional<Client> findForUpdate(Long id) { return repository.findForUpdate(id); }

    @Override
    public List<Client> list(int page, int size) {
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by("id")
        );

        return repository
                .findAllByDeletedAtIsNull(pageable)
                .getContent();
    }
}
