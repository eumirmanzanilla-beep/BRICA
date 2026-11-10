package com.brica.documents.application;

import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.domain.ReferenceAttribute;
import com.brica.documents.domain.ReferenceField;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterReferenceFieldService {
    private final ReferenceFieldStore store;

    public RegisterReferenceFieldService(ReferenceFieldStore store) {
        this.store = store;
    }

    @Transactional
    public ReferenceField register(String key, String visibleName, ReferenceAttribute attribute) {
        var field = new ReferenceField(key, visibleName, attribute);
        if (store.existsByKey(field.getKey())) {
            throw new DuplicateReferenceKeyException(field.getKey());
        }
        return store.save(field);
    }
}
