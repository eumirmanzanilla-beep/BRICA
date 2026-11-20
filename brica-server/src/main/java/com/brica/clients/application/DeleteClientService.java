package com.brica.clients.application;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;
import com.brica.server.shared.validation.domain.InputRules;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@accessPolicy.hasPermission('CLIENTS_WRITE')")
public class DeleteClientService {

    private final ClientStore clientStore;

    public DeleteClientService(ClientStore clientStore) {
        this.clientStore = clientStore;
    }

    @Transactional
    public void delete(Long id, String expectedEtag) {

        InputRules.positiveId("id", id);

        Client client = clientStore.findForUpdate(id)
                .orElseThrow(
                        () -> new ClientNotFoundException(id)
                );

        ClientRevision.check(client, expectedEtag);
        client.markDeleted(
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        clientStore.save(client);
    }
}
