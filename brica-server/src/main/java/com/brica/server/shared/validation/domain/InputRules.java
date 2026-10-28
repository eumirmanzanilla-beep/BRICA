package com.brica.server.shared.validation.domain;

import java.util.Map;

public final class InputRules {
    private InputRules() { }

    public static void requiredText(Map<String, String> errors, String field, String value, int max) {
        if (value == null || value.isBlank()) {
            errors.put(field, "El campo es obligatorio.");
        } else if (value.length() > max) {
            errors.put(field, "El campo no debe superar " + max + " caracteres.");
        }
    }

    public static void positiveId(String field, Long value) {
        if (value == null || value <= 0) {
            throw new InvalidDataException(Map.of(field, "El identificador debe ser positivo."));
        }
    }

    public static void rejectErrors(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new InvalidDataException(errors);
        }
    }
}
