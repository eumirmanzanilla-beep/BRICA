package com.brica.server.shared.validation.domain;

import java.util.Map;
import java.util.regex.Pattern;

public final class InputRules {

    private static final Pattern VALID_NAME = Pattern.compile(
        "^(?=.*[A-Za-zÑñ])[A-Za-zÑñ0-9][A-Za-zÑñ0-9_]*$"
    );

    private InputRules() { }

    public static void requiredText(Map<String, String> errors, String field, String value, int max) {
        if (value == null || value.isBlank()) {
            errors.put(field, "El campo es obligatorio.");
        } else if (value.length() > max) {
            errors.put(field, "El campo no debe superar " + max + " caracteres.");
        }
    }

    public static void validName(Map<String, String> errors, String field, String value, int max) {
        requiredText(errors, field, value, max);
        if (errors.containsKey(field)) {
            return;
        }

        if (!VALID_NAME.matcher(value).matches()) {
            errors.put(
                field,
                "El nombre solo admite letras sin acentos, "
                + "ñ, números y guión bajo; debe contener "
                + "al menos una letra y no puede empezar con '_'."
            );
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
