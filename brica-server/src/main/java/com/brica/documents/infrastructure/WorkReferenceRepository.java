package com.brica.documents.infrastructure;

import java.util.Optional;

import com.brica.documents.application.WorkReferenceData;
import com.brica.works.domain.Work;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkReferenceRepository extends Repository<Work, Long> {
    @Query("""
            SELECT new com.brica.documents.application.WorkReferenceData(w)
            FROM Work w JOIN w.client c
            WHERE w.id = :workId AND w.deletedAt IS NULL AND c.deletedAt IS NULL
            """)
    Optional<WorkReferenceData> findReferenceData(@Param("workId") Long workId);
}
