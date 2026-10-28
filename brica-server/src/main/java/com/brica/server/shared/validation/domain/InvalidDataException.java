package com.brica.server.shared.validation.domain;

import java.util.Map;

public class InvalidDataException extends RuntimeException {
    private final Map<String, String> errors;

    public InvalidDataException(Map<String, String> errors) {
        super("Hay campos que requieren corrección.");
        this.errors = Map.copyOf(errors);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
