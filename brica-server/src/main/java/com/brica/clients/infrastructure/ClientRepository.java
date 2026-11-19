
package com.brica.clients.infrastructure;

import java.util.Optional;

import com.brica.clients.domain.Client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository
        extends JpaRepository<Client, Long> {

    Optional<Client> findByIdAndDeletedAtIsNull(Long id);

    Page<Client> findAllByDeletedAtIsNull(Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Client c where c.id = :id and c.deletedAt is null")
    Optional<Client> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
