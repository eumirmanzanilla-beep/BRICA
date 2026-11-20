package com.brica.server.shared.validation.domain;

import java.math.BigDecimal;
import java.util.Map;
import com.brica.server.shared.validation.domain.InvalidDataException;

public final class ExtractionRules {
    private ExtractionRules() { }
    public static void require(boolean condition, String field, String message) {
        if (!condition) { throw new InvalidDataException(Map.of(field, message)); }
    }
    public static void text(String field, String value, int maximum, boolean required) {
        require(value != null || !required, field, "El valor es obligatorio.");
        if (value == null) { return; }
        require((!required || !value.isBlank()) && value.length() <= maximum
                && value.codePoints().noneMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t'),
                field, "Texto inválido o demasiado largo (máximo " + maximum + ").");
    }
    public static BigDecimal number(String field, String value) {
        if (value == null || value.isBlank()) { return null; }
        try {
            var number = new BigDecimal(value);
            require(number.precision() + Math.max(0, -number.scale()) <= 38 && Math.max(number.scale(), 0) <= 18
                    && number.scale() >= -38, field, "Máximo 38 dígitos y 18 decimales.");
            return number;
        } catch (NumberFormatException failure) {
            throw new InvalidDataException(Map.of(field, "Indica un decimal inequívoco, sin separador de miles."));
        }
    }
}
