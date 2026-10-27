package com.brica.clients.application;

import java.util.List;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListClientsService {

    private final ClientStore clientStore;

    public ListClientsService(ClientStore clientStore) {
        this.clientStore = clientStore;
    }

    @Transactional(readOnly = true)
    public List<Client> listClients() {
        return clientStore.list();
    }
}
