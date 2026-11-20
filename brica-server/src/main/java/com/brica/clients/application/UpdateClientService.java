
package com.brica.clients.application;
import org.springframework.security.access.prepost.PreAuthorize;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;
import com.brica.server.shared.validation.domain.InputRules;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@accessPolicy.hasPermission('CLIENTS_WRITE')")
public class UpdateClientService {

    private final ClientStore clientStore;

    public UpdateClientService(ClientStore clientStore) {
        this.clientStore = clientStore;
    }

    @Transactional
    public Client update(
            Long id,
            String expectedEtag,
            String name,
            String legalRepresentative) {

        InputRules.positiveId("id", id);

        Client client = clientStore.findForUpdate(id)
                .orElseThrow(
                        () -> new ClientNotFoundException(id)
                );

        ClientRevision.check(client, expectedEtag);
        client.updateDetails(name, legalRepresentative);

        return clientStore.save(client);
    }
    @Transactional
    public Client updateDocumentName(Long id,String expectedEtag,String value) {
        InputRules.positiveId("id",id);
        var client=clientStore.findForUpdate(id).orElseThrow(()->new ClientNotFoundException(id));
        ClientRevision.check(client,expectedEtag); client.changeDocumentName(value);
        return clientStore.save(client);
    }
}
