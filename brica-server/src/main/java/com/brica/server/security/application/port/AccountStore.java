package com.brica.server.security.application.port;

import java.util.Optional;

import com.brica.server.security.application.AccountAccess;

public interface AccountStore {
    Optional<AccountAccess> findAccess(String username);
}
