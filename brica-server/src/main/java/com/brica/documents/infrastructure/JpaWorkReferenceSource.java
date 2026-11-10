package com.brica.documents.infrastructure;

import java.util.Optional;

import com.brica.documents.application.ReferenceSourceException;
import com.brica.documents.application.WorkReferenceData;
import com.brica.documents.application.port.WorkReferenceSource;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaWorkReferenceSource implements WorkReferenceSource {
    private final WorkReferenceRepository repository;

    public JpaWorkReferenceSource(WorkReferenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<WorkReferenceData> findByWorkId(Long workId) {
        try {
            return repository.findReferenceData(workId);
        } catch (DataAccessException exception) {
            throw new ReferenceSourceException(exception);
        }
    }
}
