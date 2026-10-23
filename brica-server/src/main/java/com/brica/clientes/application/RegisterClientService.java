package com.brica.clientes.application;

import com.brica.clientes.domain.Cliente;
import com.brica.clientes.infrastructure.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service 
public class RegisterClientService{
    private final ClienteRepository  clientRepository;

    public RegisterClientService(ClienteRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional 
    public  Cliente registerClient(
        String name, 
        String legalRepresentative) {

            Cliente client = new Cliente(
                name,
                legalRepresentative
            );
        return clientRepository.save(client);
    }
}