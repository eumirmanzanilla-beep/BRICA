package com.brica.clientes.application;

import java.util.List;

import com.brica.clientes.domain.Cliente;
import com.brica.clientes.infrastructure.ClienteRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListClientsService {

    private final ClienteRepository clientRepository;

    public ListClientsService(ClienteRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listClients() {
        return clientRepository.findAll();
    }
}