package com.brica.server.config;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConfigurationTest {

    @Test
    void shouldUsePublishedComposePortByDefault() throws IOException {
        assertEquals("jdbc:postgresql://localhost:5433/brica",
                environment().getProperty("spring.datasource.url"));
    }

    @Test
    void shouldUseConfiguredPublishedPortAndDatabase() throws IOException {
        var environment = environment()
                .withProperty("POSTGRES_PORT", "5544")
                .withProperty("POSTGRES_DB", "brica_test");

        assertEquals("jdbc:postgresql://localhost:5544/brica_test",
                environment.getProperty("spring.datasource.url"));
    }

    @Test
    void shouldPreferExplicitDatabaseUrl() throws IOException {
        var environment = environment()
                .withProperty("POSTGRES_PORT", "5544")
                .withProperty("POSTGRES_DB", "local")
                .withProperty("DB_URL", "jdbc:postgresql://postgres:5432/brica");

        assertEquals("jdbc:postgresql://postgres:5432/brica",
                environment.getProperty("spring.datasource.url"));
    }

    private MockEnvironment environment() throws IOException {
        var environment = new MockEnvironment();
        var sources = new YamlPropertySourceLoader().load(
                "application", new ClassPathResource("application.yml"));
        sources.forEach(source -> environment.getPropertySources().addLast(source));
        return environment;
    }
}
