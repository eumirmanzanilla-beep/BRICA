package com.brica.documents.domain;

import java.util.Map;
import java.util.regex.Pattern;

import com.brica.server.shared.validation.domain.InvalidDataException;

public record ReferenceKey(String value) {
    private static final Pattern CANONICAL_KEY = Pattern.compile("[A-Z0-9_]{1,50}");

    public ReferenceKey {
        if (!isCanonical(value)) {
            throw new InvalidDataException(Map.of(
                    "key", "La clave debe tener de 1 a 50 caracteres A–Z, 0–9 o guion bajo."));
        }
    }

    public String marker() {
        return "{{" + value + "}}";
    }

    public static ReferenceKey fromMarker(String marker) {
        if (marker == null || marker.length() < 5 || marker.length() > 54
                || !marker.startsWith("{{") || !marker.endsWith("}}")
                || !isCanonical(marker.substring(2, marker.length() - 2))) {
            throw new InvalidDataException(Map.of(
                    "marker", "El marcador debe coincidir exactamente con {{CLAVE}}."));
        }
        return new ReferenceKey(marker.substring(2, marker.length() - 2));
    }

    private static boolean isCanonical(String value) {
        return value != null && CANONICAL_KEY.matcher(value).matches();
    }
}
