package com.brica.clients.application;

import java.util.List;

import com.brica.clients.domain.Client;
import com.brica.clients.application.port.ClientStore;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListClientsService {

    private final ClientStore clientRepository;

    public ListClientsService(ClientStore clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<Client> listClients() {
        return listClients(0, 50);
    }
    @Transactional(readOnly = true)
    public List<Client> listClients(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new com.brica.server.shared.validation.domain.InvalidDataException(
                    java.util.Map.of("pagination", "Usa page >= 0 y size entre 1 y 200."));
        }
        return clientRepository.list(page, size);
    }
}
