package com.brica.server.security.application.port;

import java.util.Optional;

public interface CurrentActor {
    Optional<String> username();
}
