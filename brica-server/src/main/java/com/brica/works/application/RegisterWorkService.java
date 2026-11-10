package com.brica.works.application;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.brica.clients.domain.Client;
import com.brica.clients.application.port.ClientStore;
import com.brica.works.domain.Work;
import com.brica.works.application.port.WorkStore;
import com.brica.clients.application.ClientNotFoundException;

@Service
@PreAuthorize("@accessPolicy.hasPermission('WORKS_WRITE')")
public class RegisterWorkService {

    private final ClientStore clientRepository;
    private final WorkStore workRepository;

    public RegisterWorkService(
            ClientStore clientRepository,
            WorkStore workRepository) {
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
        com.brica.server.shared.validation.domain.InputRules.positiveId("clientId", clientId);
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
