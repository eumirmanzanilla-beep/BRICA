package com.brica.documents.application;
import org.springframework.security.access.prepost.PreAuthorize;

import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.domain.ReferenceAttribute;
import com.brica.documents.domain.ReferenceField;
import com.brica.documents.domain.ReferenceDataType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN')")
public class RegisterReferenceFieldService {
    private final ReferenceFieldStore store;

    public RegisterReferenceFieldService(ReferenceFieldStore store) {
        this.store = store;
    }

    @Transactional
    public ReferenceField register(String key, String visibleName, ReferenceAttribute attribute) {
        var field = new ReferenceField(key, visibleName, attribute);
        return save(field);
    }

    @Transactional
    public ReferenceField registerAdditional(String key, String visibleName, ReferenceDataType type) {
        return save(ReferenceField.additional(key, visibleName, type));
    }
    @Transactional
    public ReferenceField registerInternal(String key, ReferenceDataType additionalType) {
        var attribute = ReferenceAttribute.forKey(key);
        return attribute.isPresent() ? register(key, key, attribute.get()) : registerAdditional(key, key, additionalType);
    }

    private ReferenceField save(ReferenceField field) {
        if (store.existsByKey(field.getKey())) {
            throw new DuplicateReferenceKeyException(field.getKey());
        }
        return store.save(field);
    }
}
