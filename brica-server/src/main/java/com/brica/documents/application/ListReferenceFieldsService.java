package com.brica.documents.application;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.domain.ReferenceAttribute;
import com.brica.documents.domain.ReferenceField;
import com.brica.server.shared.validation.domain.InvalidDataException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN')")
public class ListReferenceFieldsService {
    private final ReferenceFieldStore store;

    public ListReferenceFieldsService(ReferenceFieldStore store) {
        this.store = store;
    }

    public List<ReferenceAttribute> listApprovedAttributes() {
        return List.of(ReferenceAttribute.values());
    }

    @Transactional(readOnly = true)
    public List<ReferenceField> list(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new InvalidDataException(Map.of("pagination", "Página o tamaño inválidos (máximo 200)."));
        }
        return store.list(page, size);
    }
}
