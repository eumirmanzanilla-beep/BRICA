package com.brica.server.security.application;

import java.util.Map;
import java.util.Set;

import com.brica.server.security.application.port.SecurityAdministrationStore;
import com.brica.server.security.domain.AppUser;
import com.brica.server.security.domain.Permission;
import com.brica.server.security.domain.PasswordPolicy;
import com.brica.server.security.domain.Role;
import com.brica.server.shared.validation.domain.InvalidDataException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

// Registrado exclusivamente por el perfil administrativo local, no como servicio web.
public class SecurityAdministrationService {
    private final SecurityAdministrationStore store;
    private final PasswordEncoder passwords;
    public SecurityAdministrationService(SecurityAdministrationStore store, PasswordEncoder passwords) {
        this.store = store;
        this.passwords = passwords;
    }

    @Transactional
    public Role createRole(String name, int level, String description, Set<String> permissions) {
        return store.saveRole(new Role(name, level, description, approvedPermissions(permissions)));
    }

    @Transactional
    public AppUser createUser(String username, String name, Long roleId, CharSequence password) {
        PasswordPolicy.validate(password);
        var role = role(roleId);
        return store.saveUser(new AppUser(username, name, role, passwords.encode(password)));
    }

    @Transactional
    public void setUserActive(String username, boolean active) {
        var user = user(username);
        user.setActive(active);
        store.saveUser(user);
    }

    @Transactional
    public void resetPassword(String username, CharSequence password) {
        PasswordPolicy.validate(password);
        var user = user(username);
        user.replacePasswordHash(passwords.encode(password));
        store.saveUser(user);
    }

    @Transactional
    public void setRoleActive(Long id, boolean active) {
        var role = role(id);
        role.setActive(active);
        store.saveRole(role);
    }

    @Transactional
    public void replaceRolePermissions(Long id, Set<String> permissions) {
        var role = role(id);
        role.replacePermissions(approvedPermissions(permissions));
        store.saveRole(role);
    }

    private Set<Permission> approvedPermissions(Set<String> keys) {
        var permissions = store.findPermissions(keys);
        if (permissions.size() != keys.size()) {
            throw new InvalidDataException(Map.of("permissions", "Hay permisos no registrados."));
        }
        return permissions;
    }
    private Role role(Long id) {
        return store.findRoleForUpdate(id).orElseThrow(() -> new InvalidDataException(Map.of("roleId", "Rol inexistente.")));
    }
    private AppUser user(String username) {
        return store.findUserForUpdate(username).orElseThrow(() -> new InvalidDataException(Map.of("username", "Usuario inexistente.")));
    }
}
