package com.brica.server.security.infrastructure;

import java.util.Optional;
import jakarta.persistence.LockModeType;

import com.brica.server.security.domain.AppUser;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    @EntityGraph(attributePaths = {"role", "role.permissions"})
    Optional<AppUser> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM AppUser u WHERE u.username = :username")
    Optional<AppUser> findForUpdate(@Param("username") String username);
}
