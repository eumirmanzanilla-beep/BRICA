package com.brica.works.application;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.brica.clientes.domain.Cliente;
import com.brica.clientes.infrastructure.ClienteRepository;
import com.brica.works.domain.Work;
import com.brica.works.infrastructure.WorkRepository;
import com.brica.clientes.application.ClientNotFoundException;  

@Service 
public class RegisterWorkService {

    private final ClienteRepository clientRepository;
    private final WorkRepository workRepository;

    public RegisterWorkService(
            ClienteRepository clientRepository,
            WorkRepository workRepository) {
                this.clientRepository = clientRepository;
                this.workRepository = workRepository;
            }

    @Transactional
    public Work registerWork(
        Long clientId,
        String name,
        String location,
        String description,
        LocalDate startDate,
        LocalDate endDate) {
            Cliente client = clientRepository.findById(clientId)
                .orElseThrow(() ->
                    new ClientNotFoundException(clientId)
                );

        Work work = new Work(
            client,
            name,
            location,
            description,
            startDate,
            endDate
        );

        return workRepository.save(work);
    }
}