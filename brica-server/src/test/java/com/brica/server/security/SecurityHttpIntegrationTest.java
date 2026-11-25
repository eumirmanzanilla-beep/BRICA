package com.brica.server.security;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.brica.clients.application.RegisterClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "brica.security.allow-insecure-http=true")
class SecurityHttpIntegrationTest {
    @Container @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwords;
    @Autowired private RegisterClientService registerClient;
    @Autowired private ApplicationContext application;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void standardRolesEnforceReadOnlyAndAdministrativeAccess() throws Exception {
        for (String role : List.of("Gerencial", "Administrativo", "Practicante", "Invitado")) {
            String username = "RoleTest" + UUID.randomUUID().toString().replace("-", "");
            jdbc.update("""
                    INSERT INTO app_users (role_id, username, password_hash, name, status)
                    SELECT id_role, ?, ?, ?, 'ACTIVE' FROM roles WHERE name = ?
                    """, username, passwords.encode("TestUser123"), username, role);
            var session = login(username);
            assertEquals(200, session.get("/clients").statusCode(), role);
            assertEquals(200, session.get("/works").statusCode(), role);
            boolean administrator = role.equals("Gerencial") || role.equals("Administrativo");
            assertEquals(administrator ? 201 : 403, session.json("POST", "/reference-fields", fieldBody(), true).statusCode(), role);
            assertEquals(administrator ? 201 : 403, session.json("POST", "/clients",
                    "{\"name\":\"Cliente_prueba\",\"legalRepresentative\":\"Representante\"}", true).statusCode(), role);
        }
    }

    @Test
    void shouldKeepOnlyHealthAndLoginPreparationPublic() throws Exception {
        var anonymous = new SessionClient();
        assertEquals(200, anonymous.get("/health").statusCode());
        for (String path : List.of("/clients", "/works", "/reference-fields", "/auth/session")) {
            var response = anonymous.get(path);
            assertEquals(401, response.statusCode(), response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/problem+json"));
            assertEquals("AUTHENTICATION_REQUIRED", JSON.readTree(response.body()).path("code").asText());
        }
        anonymous.refreshCsrf();
        assertEquals(401, anonymous.json("POST", "/reference-fields", fieldBody(), true).statusCode());
        assertFalse(application.containsBean("securityAdministrationService"));
    }

    @Test
    void shouldAuthenticateExactUsernameRotateSessionAndNeverExposeHash() throws Exception {
        String username = account("CaseUser", "DOCUMENT_CONFIG_ADMIN");
        var session = new SessionClient();
        session.refreshCsrf();
        String oldCookie = session.cookie();
        var wrong = session.login(username.toLowerCase(), "TestUser123");
        assertEquals(401, wrong.statusCode());
        session.refreshCsrf();
        var correct = session.login(username, "TestUser123");
        assertEquals(200, correct.statusCode(), correct.body());
        assertNotEquals(oldCookie, session.cookie());
        assertFalse(correct.body().contains("password"));
        assertTrue(correct.headers().allValues("Set-Cookie").stream().anyMatch(v -> v.contains("HttpOnly")));
        assertFalse(correct.headers().allValues("Set-Cookie").stream().anyMatch(v -> v.contains("Max-Age=")));
        assertEquals(username, JSON.readTree(session.get("/auth/session").body()).path("username").asText());
        session.refreshCsrf();
        assertEquals(401, session.login(" " + username, "TestUser123").statusCode());
    }

    @Test
    void shouldSeparateDocumentAdministrationFromGlobalValueReadPermission() throws Exception {
        var admin = login(account("Admin", "DOCUMENT_CONFIG_ADMIN"));
        var created = admin.json("POST", "/reference-fields", fieldBody(), true);
        assertEquals(201, created.statusCode(), created.body());
        long fieldId = JSON.readTree(created.body()).path("id").asLong();
        long first = work("Obra_A", "Cliente A");
        long second = work("Obra_B", "Cliente B");
        String body = "{\"workId\":" + first + ",\"fieldIds\":[" + fieldId + "]}";
        assertEquals(403, admin.json("POST", "/reference-values/resolve", body, true).statusCode());
        var reader = login(account("Reader", "DOCUMENT_VALUES_READ"));
        var a = reader.json("POST", "/reference-values/resolve", body, true);
        var b = reader.json("POST", "/reference-values/resolve",
                "{\"workId\":" + second + ",\"fieldIds\":[" + fieldId + "]}", true);
        assertEquals(200, a.statusCode(), a.body());
        assertEquals(200, b.statusCode(), b.body());
        assertEquals("Obra_A", JSON.readTree(a.body()).path(0).path("textValue").asText());
        assertEquals("Obra_B", JSON.readTree(b.body()).path(0).path("textValue").asText());
        long before = jdbc.queryForObject("SELECT count(*) FROM reference_fields", Long.class);
        assertEquals(403, reader.json("POST", "/reference-fields", fieldBody(), true).statusCode());
        assertEquals(403, reader.json("PUT", "/reference-fields/" + fieldId,
                "{\"visibleName\":\"No autorizado\"}", true).statusCode());
        assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM reference_fields", Long.class));
        assertEquals(403, login(account("Neither")).json("POST", "/reference-values/resolve", body, true).statusCode());
        assertEquals(200, login(account("Both", "DOCUMENT_VALUES_READ", "DOCUMENT_CONFIG_ADMIN"))
                .json("POST", "/reference-values/resolve", body, true).statusCode());
    }

    @Test
    void shouldProtectExistingModulesAndApplicationCallsWithoutGrantingByHierarchy() throws Exception {
        String username = account("ClientReader", "CLIENTS_READ");
        var reader = login(username);
        assertEquals(200, reader.get("/clients").statusCode());
        assertEquals(403, reader.get("/works").statusCode());
        assertEquals(403, reader.json("POST", "/clients", "{\"name\":\"X\",\"legalRepresentative\":\"Y\"}", true).statusCode());
        jdbc.update("UPDATE roles SET hierarchy_level = 999 WHERE name = ?", "test-role-" + username);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(username, null, List.of()));
        SecurityContextHolder.setContext(context);
        try {
            assertThrows(AccessDeniedException.class, () -> registerClient.registerClient("Bloqueado", "Representante"));
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    void shouldObservePermissionRevocationAndInvalidateDisabledAccounts() throws Exception {
        String username = account("Revoked", "CLIENTS_READ");
        var session = login(username);
        assertEquals(200, session.get("/clients").statusCode());
        jdbc.update("DELETE FROM role_permissions WHERE role_id = (SELECT role_id FROM app_users WHERE username = ?)", username);
        assertEquals(403, session.get("/clients").statusCode());
        jdbc.update("UPDATE roles SET status = 0 WHERE id_role = (SELECT role_id FROM app_users WHERE username = ?)", username);
        assertEquals(401, session.get("/auth/session").statusCode());
        assertEquals(401, session.get("/clients").statusCode());
        session.refreshCsrf();
        assertEquals(401, session.login(username, "TestUser123").statusCode());
        String disabledUser = account("Disabled", "CLIENTS_READ");
        var other = login(disabledUser);
        jdbc.update("UPDATE app_users SET status = 'INACTIVE' WHERE username = ?", disabledUser);
        assertEquals(401, other.get("/clients").statusCode());
    }

    @Test
    void shouldRequireCsrfAndInvalidateSessionOnExplicitLogout() throws Exception {
        var session = login(account("Csrf", "DOCUMENT_CONFIG_ADMIN"));
        long before = jdbc.queryForObject("SELECT count(*) FROM reference_fields", Long.class);
        assertEquals(403, session.json("POST", "/reference-fields", fieldBody(), false).statusCode());
        assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM reference_fields", Long.class));
        assertEquals(403, session.json("POST", "/auth/logout", "", false).statusCode());
        assertEquals(200, session.get("/auth/session").statusCode());
        assertEquals(204, session.json("POST", "/auth/logout", "", true).statusCode());
        assertEquals(401, session.get("/auth/session").statusCode());
    }

    private String account(String prefix, String... permissions) {
        String username = prefix + UUID.randomUUID().toString().replace("-", "");
        SecurityTestData.install(jdbc, passwords, username, permissions);
        return username;
    }
    private SessionClient login(String username) throws Exception {
        var session = new SessionClient();
        session.refreshCsrf();
        var response = session.login(username, "TestUser123");
        assertEquals(200, response.statusCode(), response.body());
        session.refreshCsrf();
        return session;
    }
    private String fieldBody() {
        return "{\"key\":\"K_" + UUID.randomUUID().toString().replace("-", "").toUpperCase()
                + "\",\"visibleName\":\"Obra\",\"attribute\":\"WORK_NAME\"}";
    }
    private long work(String workName, String clientName) {
        Long client = jdbc.queryForObject("INSERT INTO cliente (nombre, representante_legal) VALUES (?, 'Representante') RETURNING id_cliente", Long.class, clientName);
        return jdbc.queryForObject("INSERT INTO works (client_id, name, location, start_date) VALUES (?, ?, 'Mérida', '2026-01-01') RETURNING id_work", Long.class, client, workName);
    }

    private class SessionClient {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();
        private String header;
        private String token;
        String cookie() { return cookies.getCookieStore().getCookies().toString(); }
        URI uri(String path) { return URI.create("http://localhost:" + port + "/api/v1" + path); }
        HttpResponse<String> get(String path) throws Exception {
            return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        void refreshCsrf() throws Exception {
            var response = get("/auth/csrf");
            assertEquals(200, response.statusCode(), response.body());
            JsonNode data = JSON.readTree(response.body());
            header = data.path("headerName").asText();
            token = data.path("token").asText();
        }
        HttpResponse<String> login(String username, String password) throws Exception {
            return http.send(HttpRequest.newBuilder(uri("/auth/login"))
                    .header("Content-Type", "application/x-www-form-urlencoded").header(header, token)
                    .POST(HttpRequest.BodyPublishers.ofString("username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                            + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8))).build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> json(String method, String path, String body, boolean includeCsrf) throws Exception {
            var request = HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
            if (includeCsrf) request.header(header, token);
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
