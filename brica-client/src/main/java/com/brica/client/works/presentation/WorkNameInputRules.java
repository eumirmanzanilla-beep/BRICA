package com.brica.client.works.presentation;

import java.util.regex.Pattern;

/** Rules for the business identifier shown as the name of a work. */
final class WorkNameInputRules {
    private static final Pattern VALID_NAME = Pattern.compile("^[\\p{L}][\\p{L}\\p{N} _-]*$");
    private static final String MESSAGE = "Usa letras, números, espacios y guiones; el nombre debe iniciar con una letra.";

    private WorkNameInputRules() { }

    static String error(String value) {
        if (value == null || value.isBlank()) {
            return "El nombre de la obra es obligatorio.";
        }
        if (value.length() > 255) {
            return "El nombre de la obra no debe superar 255 caracteres.";
        }
        return VALID_NAME.matcher(value).matches() ? "" : MESSAGE;
    }

    static void require(String value) {
        String error = error(value);
        if (!error.isEmpty()) {
            throw new IllegalArgumentException(error);
        }
    }
}
