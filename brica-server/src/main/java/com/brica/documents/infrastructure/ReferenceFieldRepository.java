package com.brica.documents.infrastructure;

import com.brica.documents.domain.ReferenceField;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenceFieldRepository extends JpaRepository<ReferenceField, Long> {
    boolean existsByKey(String key);
    java.util.List<ReferenceField> findByKeyIn(java.util.Collection<String> keys);
}
