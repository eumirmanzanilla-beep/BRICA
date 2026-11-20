package com.brica.server.security.application;

import java.util.Set;

public record AccountAccess(Long id, String username, String name, Long roleId, String roleName,
        boolean enabled, Set<String> permissions) {
    public AccountAccess { permissions = Set.copyOf(permissions); }
}
