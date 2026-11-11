package com.brica.documents.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.brica.documents.application.DuplicateReferenceKeyException;
import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.domain.ReferenceField;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaReferenceFieldStore implements ReferenceFieldStore {
    private final ReferenceFieldRepository repository;

    public JpaReferenceFieldStore(ReferenceFieldRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReferenceField save(ReferenceField field) {
        try {
            return repository.saveAndFlush(field);
        } catch (DataIntegrityViolationException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation
                        && "uq_reference_fields_key".equals(violation.getConstraintName())) {
                    throw new DuplicateReferenceKeyException(field.getKey());
                }
            }
            throw exception;
        }
    }

    @Override
    public boolean existsByKey(String key) {
        return repository.existsByKey(key);
    }

    @Override
    public Optional<ReferenceField> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ReferenceField> findAllById(Collection<Long> ids) {
        return repository.findAllById(ids);
    }

    @Override
    public List<ReferenceField> findAllByKey(Collection<String> keys) {
        return keys.isEmpty() ? List.of() : repository.findByKeyIn(keys);
    }

    @Override
    public List<ReferenceField> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("id"))).getContent();
    }
}
