package com.brica.server.security.infrastructure;

import java.util.Optional;
import java.util.stream.Collectors;

import com.brica.server.security.application.AccountAccess;
import com.brica.server.security.application.port.AccountStore;
import com.brica.server.security.domain.Permission;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaAccountStore implements AccountStore {
    private final AppUserRepository users;
    public JpaAccountStore(AppUserRepository users) { this.users = users; }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountAccess> findAccess(String username) {
        return users.findByUsername(username).map(user -> new AccountAccess(user.getId(),
                user.getUsername(), user.getName(), user.getRole().getId(), user.getRole().getName(),
                user.isActive() && user.getRole().isActive(), user.getRole().getPermissions().stream()
                        .map(Permission::getKey).collect(Collectors.toSet())));
    }
}
