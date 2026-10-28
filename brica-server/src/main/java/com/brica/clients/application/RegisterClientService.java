package com.brica.clients.application;

import com.brica.clients.domain.Client;
import com.brica.clients.application.port.ClientStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterClientService{
    private final ClientStore  clientRepository;

    public RegisterClientService(ClientStore clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public  Client registerClient(
        String name,
        String legalRepresentative) {

            Client client = new Client(
                name,
                legalRepresentative
            );
        return clientRepository.save(client);
    }
}
