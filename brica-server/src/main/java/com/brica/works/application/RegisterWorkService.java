package com.brica.works.application;

import java.time.LocalDate;

import com.brica.clients.application.ClientNotFoundException;
import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;
import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterWorkService {

    private final ClientStore clientStore;
    private final WorkStore workStore;

    public RegisterWorkService(
            ClientStore clientStore,
            WorkStore workStore) {

        this.clientStore = clientStore;
        this.workStore = workStore;
    }

    @Transactional
    public Work registerWork(
            Long clientId,
            String name,
            String location,
            String description,
            LocalDate startDate,
            LocalDate endDate) {

        Client client = clientStore.findById(clientId)
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

        return workStore.save(work);
    }
}
