
package com.brica.clients.application;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;
import com.brica.server.shared.validation.domain.InputRules;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultClientService {

    private final ClientStore clientStore;

    public ConsultClientService(ClientStore clientStore) {
        this.clientStore = clientStore;
    }

    @Transactional(readOnly = true)
    public Client get(Long id) {

        InputRules.positiveId("id", id);

        return clientStore.findById(id)
                .orElseThrow(
                        () -> new ClientNotFoundException(id)
                );
    }
}
