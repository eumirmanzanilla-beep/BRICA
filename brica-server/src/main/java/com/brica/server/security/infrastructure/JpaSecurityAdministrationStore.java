package com.brica.server.security.infrastructure;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.brica.server.security.application.port.SecurityAdministrationStore;
import com.brica.server.security.domain.AppUser;
import com.brica.server.security.domain.Permission;
import com.brica.server.security.domain.Role;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSecurityAdministrationStore implements SecurityAdministrationStore {
    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    public JpaSecurityAdministrationStore(AppUserRepository users, RoleRepository roles, PermissionRepository permissions) {
        this.users = users;
        this.roles = roles;
        this.permissions = permissions;
    }
    public Set<Permission> findPermissions(Set<String> keys) { return new HashSet<>(permissions.findAllByKeyIn(keys)); }
    public Optional<Role> findRoleForUpdate(Long id) { return roles.findForUpdate(id); }
    public Optional<AppUser> findUserForUpdate(String username) { return users.findForUpdate(username); }
    public Role saveRole(Role role) { return roles.saveAndFlush(role); }
    public AppUser saveUser(AppUser user) { return users.saveAndFlush(user); }
}
