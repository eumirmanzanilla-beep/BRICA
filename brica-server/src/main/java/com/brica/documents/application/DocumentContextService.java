package com.brica.documents.application;

import com.brica.documents.application.port.DocumentConfigurationStore;
import com.brica.server.shared.validation.domain.InputRules;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class DocumentContextService {
    private final DocumentConfigurationStore store;
    public DocumentContextService(DocumentConfigurationStore store) { this.store = store; }
    @PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN') and "
            + "(#workId == null or @accessPolicy.hasPermission('WORKS_READ')) and "
            + "(#workId != null or #clientId == null or @accessPolicy.hasPermission('CLIENTS_READ'))")
    public Long client(@org.springframework.security.core.parameters.P("clientId") Long clientId,
            @org.springframework.security.core.parameters.P("workId") Long workId) {
        if (workId != null) {
            InputRules.positiveId("workId", workId);
            Long actual = store.workClient(workId).orElseThrow(() -> new DocumentException(404, "WORK_NOT_FOUND", "Selecciona una obra vigente."));
            if (clientId != null && !clientId.equals(actual)) { throw new DocumentException(400, "DOCUMENT_SCOPE_MISMATCH", "La obra no pertenece al cliente."); }
            return actual;
        }
        if (clientId != null) {
            InputRules.positiveId("clientId", clientId);
            if (!store.activeClient(clientId)) { throw new DocumentException(400, "PACKAGE_CLIENT_UNAVAILABLE", "Selecciona un cliente vigente."); }
        }
        return clientId;
    }
}
