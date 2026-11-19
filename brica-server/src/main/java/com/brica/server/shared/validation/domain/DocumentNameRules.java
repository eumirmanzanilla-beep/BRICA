package com.brica.server.shared.validation.domain;

import java.util.Map;

public final class DocumentNameRules {
    private DocumentNameRules() { }
    public static String normalize(String value) {
        if (value == null) { return null; }
        if (value.length() > 1000 || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new InvalidDataException(Map.of("documentName", "Máximo 1000 caracteres, sin caracteres de control."));
        }
        return value.isBlank() ? null : value;
    }
}
