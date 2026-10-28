package com.brica.works.infrastructure;

import com.brica.works.domain.Work;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface WorkRepository extends JpaRepository<Work, Long> {

    @EntityGraph(attributePaths = "client")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Work o where o.id = :id")
    Optional<Work> findForUpdate(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = "client")
    org.springframework.data.domain.Page<Work> findAll(org.springframework.data.domain.Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "client")
    Optional<Work> findById(Long id);

}
