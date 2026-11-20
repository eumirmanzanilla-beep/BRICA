package com.brica.server.security;

import com.brica.server.security.domain.PasswordPolicy;
import com.brica.server.security.infrastructure.PasswordConfiguration;
import com.brica.server.shared.validation.domain.InvalidDataException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    @Test
    void shouldRequireEightCharactersUppercaseAndNumberWithoutTrimming() {
        PasswordPolicy.validate("Abcdefg1");
        for (String password : new String[] {"Abcdef1", "abcdefg1", "Abcdefgh", "Á".repeat(37) + "1"}) {
            assertThrows(InvalidDataException.class, () -> PasswordPolicy.validate(password));
        }
        PasswordPolicy.validate("A" + "a".repeat(70) + "1");
        assertThrows(InvalidDataException.class, () -> PasswordPolicy.validate("A" + "a".repeat(71) + "1"));
    }

    @Test
    void shouldHashWithCostTenAndNeverAcceptTruncatedPassword() {
        PasswordEncoder encoder = new PasswordConfiguration().passwordEncoder();
        String hash = encoder.encode("Abcdefg1");
        assertTrue(hash.startsWith("{bcrypt}$2a$10$"), hash);
        assertTrue(encoder.matches("Abcdefg1", hash));
        assertFalse(encoder.matches("abcdefg1", hash));
        String maximum = "A" + "a".repeat(70) + "1";
        assertFalse(encoder.matches(maximum + "extra", encoder.encode(maximum)));
        assertNotEquals(hash, encoder.encode("Abcdefg1"));
    }
}
