package com.brica.clients.application;

import com.brica.clients.domain.Client;
import com.brica.clients.infrastructure.ClientRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterClientService {

    private final ClientRepository clientRepository;

    public RegisterClientService(
            ClientRepository clientRepository) {

        this.clientRepository = clientRepository;
    }

    @Transactional
    public Client registerClient(
            String name,
            String legalRepresentative) {

        Client client = new Client(
            name,
            legalRepresentative
        );

        return clientRepository.save(client);
    }
}