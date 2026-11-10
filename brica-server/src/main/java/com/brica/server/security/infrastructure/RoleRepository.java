package com.brica.server.security.infrastructure;

import java.util.Optional;

import com.brica.server.security.domain.Role;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Role r WHERE r.id = :id")
    Optional<Role> findForUpdate(@Param("id") Long id);
}
