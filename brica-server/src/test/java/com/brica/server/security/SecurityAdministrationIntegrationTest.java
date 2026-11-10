package com.brica.server.security;

import java.util.Set;
import java.util.UUID;

import com.brica.server.security.application.SecurityAdministrationService;
import com.brica.server.security.application.port.AccountStore;
import com.brica.server.shared.validation.domain.InvalidDataException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(SecurityAdministrationService.class)
class SecurityAdministrationIntegrationTest {
    @Container @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");
    @Autowired private SecurityAdministrationService admin;
    @Autowired private AccountStore accounts;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;

    @Test
    void shouldProvisionExactAccountsAndHashOnlyValidatedPasswords() {
        String username = "Admin" + UUID.randomUUID();
        var role = admin.createRole("Rol-" + username, 1, "Administración de prueba", Set.of("CLIENTS_READ"));
        long before = jdbc.queryForObject("SELECT count(*) FROM app_users", Long.class);
        assertThrows(InvalidDataException.class, () -> admin.createUser(username, "Nombre", role.getId(), "weak"));
        assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM app_users", Long.class));
        var user = admin.createUser(username, "Nombre", role.getId(), "Abcdefg1");
        assertNotNull(user.getId());
        String hash = jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username = ?", String.class, username);
        assertTrue(passwords.matches("Abcdefg1", hash));
        assertTrue(hash.startsWith("{bcrypt}$2a$10$"));
        assertTrue(accounts.findAccess(username).orElseThrow().enabled());
        assertTrue(accounts.findAccess(username.toLowerCase()).isEmpty());
        admin.resetPassword(username, "Updated1");
        hash = jdbc.queryForObject("SELECT password_hash FROM app_users WHERE username = ?", String.class, username);
        assertFalse(passwords.matches("Abcdefg1", hash));
        assertTrue(passwords.matches("Updated1", hash));
    }

    @Test
    void shouldApplyRolePermissionAndStateChangesWithoutDeletingAccounts() {
        String username = "State" + UUID.randomUUID();
        var role = admin.createRole("Rol-" + username, 999, null, Set.of());
        admin.createUser(username, "Nombre", role.getId(), "Abcdefg1");
        assertTrue(accounts.findAccess(username).orElseThrow().permissions().isEmpty());
        assertThrows(InvalidDataException.class, () -> admin.replaceRolePermissions(role.getId(), Set.of("UNKNOWN")));
        assertTrue(accounts.findAccess(username).orElseThrow().permissions().isEmpty());
        admin.replaceRolePermissions(role.getId(), Set.of("DOCUMENT_VALUES_READ"));
        assertEquals(Set.of("DOCUMENT_VALUES_READ"), accounts.findAccess(username).orElseThrow().permissions());
        admin.setRoleActive(role.getId(), false);
        assertFalse(accounts.findAccess(username).orElseThrow().enabled());
        admin.setRoleActive(role.getId(), true);
        admin.setUserActive(username, false);
        assertFalse(accounts.findAccess(username).orElseThrow().enabled());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM app_users WHERE username = ?", Integer.class, username));
    }
}
