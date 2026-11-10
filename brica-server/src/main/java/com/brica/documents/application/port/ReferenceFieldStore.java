package com.brica.documents.application.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.brica.documents.domain.ReferenceField;

public interface ReferenceFieldStore {
    ReferenceField save(ReferenceField field);
    boolean existsByKey(String key);
    Optional<ReferenceField> findById(Long id);
    List<ReferenceField> findAllById(Collection<Long> ids);
    List<ReferenceField> list(int page, int size);
}
