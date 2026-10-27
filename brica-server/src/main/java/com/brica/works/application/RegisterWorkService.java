package com.brica.works.application;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.brica.clients.application.ClientNotFoundException;
import com.brica.clients.domain.Client;
import com.brica.clients.infrastructure.ClientRepository;
import com.brica.works.domain.Work;
import com.brica.works.infrastructure.WorkRepository;  

@Service 
public class RegisterWorkService {

    private final ClientRepository clientRepository;
    private final WorkRepository workRepository;

    public RegisterWorkService(
            ClientRepository clientRepository,
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
            Client client = clientRepository.findById(clientId)
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