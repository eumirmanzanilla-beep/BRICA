
package com.brica.clients.application;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;
import com.brica.server.shared.validation.domain.InputRules;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateClientService {

    private final ClientStore clientStore;

    public UpdateClientService(ClientStore clientStore) {
        this.clientStore = clientStore;
    }

    @Transactional
    public Client update(
            Long id,
            String name,
            String legalRepresentative) {

        InputRules.positiveId("id", id);

        Client client = clientStore.findById(id)
                .orElseThrow(
                        () -> new ClientNotFoundException(id)
                );

        client.updateDetails(name, legalRepresentative);

        return clientStore.save(client);
    }
}
