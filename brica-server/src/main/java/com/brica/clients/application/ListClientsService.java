package com.brica.clients.application;

import java.util.List;

import com.brica.clients.domain.Client;
import com.brica.clients.infrastructure.ClientRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListClientsService {

    private final ClientRepository clientRepository;

    public ListClientsService(
            ClientRepository clientRepository) {

        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<Client> listClients() {
        return clientRepository.findAll();
    }
}