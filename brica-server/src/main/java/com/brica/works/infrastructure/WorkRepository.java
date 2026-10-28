
package com.brica.works.infrastructure;

import java.util.Optional;

import com.brica.works.domain.Work;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkRepository
        extends JpaRepository<Work, Long> {

    @EntityGraph(attributePaths = "client")
    Optional<Work> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = "client")
    Page<Work> findAllByDeletedAtIsNull(Pageable pageable);

    @EntityGraph(attributePaths = "client")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT w
            FROM Work w
            WHERE w.id = :id
              AND w.deletedAt IS NULL
            """)
    Optional<Work> findForUpdate(@Param("id") Long id);
}
