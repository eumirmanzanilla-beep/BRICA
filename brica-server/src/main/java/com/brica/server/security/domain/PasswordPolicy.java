package com.brica.server.security.domain;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.brica.server.shared.validation.domain.InvalidDataException;

public final class PasswordPolicy {
    private PasswordPolicy() { }

    public static void validate(CharSequence password) {
        String value = password == null ? "" : password.toString();
        if (value.codePointCount(0, value.length()) < 8
                || !value.codePoints().anyMatch(Character::isUpperCase)
                || !value.codePoints().anyMatch(Character::isDigit)
                || exceedsBcryptLimit(value)) {
            throw new InvalidDataException(Map.of("password",
                    "La contraseña requiere al menos 8 caracteres, una mayúscula y un número; máximo 72 bytes UTF-8."));
        }
    }

    public static boolean exceedsBcryptLimit(CharSequence value) {
        return value.toString().getBytes(StandardCharsets.UTF_8).length > 72;
    }
}
