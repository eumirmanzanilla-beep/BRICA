package com.brica.clients.application;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterClientService {

    private final ClientStore clientStore;

    public RegisterClientService(ClientStore clientStore) {
        this.clientStore = clientStore;
    }

    @Transactional
    public Client registerClient(
            String name,
            String legalRepresentative) {

        Client client = new Client(
            name,
            legalRepresentative
        );

        return clientStore.save(client);
    }
}
