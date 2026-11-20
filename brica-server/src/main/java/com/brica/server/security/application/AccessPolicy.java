package com.brica.server.security.application;

import com.brica.server.security.application.port.AccountStore;
import com.brica.server.security.application.port.CurrentActor;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;

@Service("accessPolicy")
public class AccessPolicy {
    private final AccountStore accounts;
    private final CurrentActor actor;

    public AccessPolicy(AccountStore accounts, CurrentActor actor) {
        this.accounts = accounts;
        this.actor = actor;
    }

    public AccountAccess current() {
        return actor.username().flatMap(accounts::findAccess).filter(AccountAccess::enabled)
                .orElseThrow(() -> new InsufficientAuthenticationException("Inicia sesión con una cuenta activa."));
    }

    public boolean hasPermission(String permission) {
        return current().permissions().contains(permission);
    }
}
