package com.brica.documents.application;
import org.springframework.security.access.prepost.PreAuthorize;

import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.domain.ReferenceField;
import com.brica.server.shared.validation.domain.InputRules;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN')")
public class UpdateReferenceFieldService {
    private final ReferenceFieldStore store;

    public UpdateReferenceFieldService(ReferenceFieldStore store) {
        this.store = store;
    }

    @Transactional
    public ReferenceField rename(Long id, String visibleName) {
        InputRules.positiveId("fieldId", id);
        var field = store.findById(id).orElseThrow(() -> new ReferenceFieldNotFoundException(id));
        field.rename(visibleName);
        return store.save(field);
    }

    @Transactional
    public void delete(Long id) {
        InputRules.positiveId("fieldId", id);
        if (store.findById(id).isEmpty()) { throw new ReferenceFieldNotFoundException(id); }
        store.delete(id);
    }
    @Transactional
    public ReferenceField alignName(Long id) {
        InputRules.positiveId("fieldId", id);
        var field = store.findById(id).orElseThrow(() -> new ReferenceFieldNotFoundException(id));
        field.rename(field.getKey()); return store.save(field);
    }
}
