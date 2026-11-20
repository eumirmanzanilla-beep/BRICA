package com.brica.server.security.application.port;

import java.util.Optional;
import java.util.Set;

import com.brica.server.security.domain.AppUser;
import com.brica.server.security.domain.Permission;
import com.brica.server.security.domain.Role;

public interface SecurityAdministrationStore {
    Set<Permission> findPermissions(Set<String> keys);
    Optional<Role> findRoleForUpdate(Long id);
    Optional<AppUser> findUserForUpdate(String username);
    Role saveRole(Role role);
    AppUser saveUser(AppUser user);
}
