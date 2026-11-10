package com.brica.server.security.infrastructure;

import java.util.Collection;
import java.util.List;

import com.brica.server.security.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findAllByKeyIn(Collection<String> keys);
}
