package com.brica.server.security.infrastructure;

import java.util.Map;

import com.brica.server.security.domain.PasswordPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfiguration {
    @Bean
    public PasswordEncoder passwordEncoder() {
        var bcrypt = new BCryptPasswordEncoder(10);
        PasswordEncoder bounded = new PasswordEncoder() {
            @Override public String encode(CharSequence raw) {
                if (raw == null || PasswordPolicy.exceedsBcryptLimit(raw)) {
                    throw new IllegalArgumentException("La contraseña supera el límite técnico de BCrypt.");
                }
                return bcrypt.encode(raw);
            }
            @Override public boolean matches(CharSequence raw, String encoded) {
                return raw != null && !PasswordPolicy.exceedsBcryptLimit(raw) && bcrypt.matches(raw, encoded);
            }
        };
        return new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", bounded));
    }
}
