package com.brica.contracts.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP + real PostgreSQL, with no external URL or credentials. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "brica.security.allow-insecure-http=true")
class ContractDockerHttpIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL)).build();
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwords;
    private String csrfHeader;
    private String csrfToken;

    @org.junit.jupiter.api.BeforeEach
    void authenticateRealUser() throws Exception {
        com.brica.server.security.SecurityTestData.installAdmin(jdbc, passwords);
        refreshCsrf();
        var login = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/x-www-form-urlencoded").header(csrfHeader, csrfToken)
                .POST(HttpRequest.BodyPublishers.ofString("username=IntegrationAdmin&password=TestUser123")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, login.statusCode(), login.body());
        refreshCsrf();
    }

    private void refreshCsrf() throws Exception {
        var response = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/csrf"))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        var token = JSON.readTree(response.body());
        csrfHeader = token.path("headerName").asText();
        csrfToken = token.path("token").asText();
    }

    @Test
    void shouldPersistNullJsonAndRejectInvalidContractWithoutPartialWrite() throws Exception {
        long work = createWork();
        long before = jdbc.queryForObject("select count(*) from contracts", Long.class);
        var response = send("POST", "/contracts", """
                {"workId":%d,"data":{"amount":null,"nested":{"unknown":null},"items":[null,1]}}
                """.formatted(work), null);
        assertEquals(201, response.statusCode(), response.body());
        JsonNode data = JSON.readTree(response.body()).path("data");
        assertTrue(data.path("amount").isNull());
        assertTrue(data.path("nested").path("unknown").isNull());
        long id = JSON.readTree(response.body()).path("id").asLong();
        assertEquals(data, JSON.readTree(jdbc.queryForObject(
                "select data::text from contracts where id_contract = ?", String.class, id)));
        assertEquals(before + 1, jdbc.queryForObject("select count(*) from contracts", Long.class));

        var invalid = send("POST", "/contracts", "{\"workId\":" + work + ",\"data\":{\" \":1}}", null);
        assertEquals(400, invalid.statusCode(), invalid.body());
        assertEquals("VALIDATION_ERROR", JSON.readTree(invalid.body()).path("code").asText());
        assertEquals(before + 1, jdbc.queryForObject("select count(*) from contracts", Long.class));
        assertEquals(404, send("POST", "/contracts", "{\"workId\":9223372036854775807}", null).statusCode());
    }

    @Test
    void shouldCorrectOnlyGeneralInformationAndDetectConcurrentChanges() throws Exception {
        long id = createWork();
        var original = send("GET", "/works/" + id, null, null);
        assertEquals(200, original.statusCode());
        String etag = original.headers().firstValue("ETag").orElseThrow();
        String correction = "{\"name\":\"Corregida\",\"location\":\"Mérida\",\"description\":null}";
        assertEquals(428, send("PUT", "/works/" + id + "/general-information", correction, null).statusCode());
        var corrected = send("PUT", "/works/" + id + "/general-information", correction, etag);
        assertEquals(200, corrected.statusCode(), corrected.body());
        String updatedTag = corrected.headers().firstValue("ETag").orElseThrow();
        assertNotEquals(etag, updatedTag);
        assertEquals("2026-01-01", JSON.readTree(corrected.body()).path("startDate").asText());
        assertEquals(412, send("PUT", "/works/" + id + "/general-information", correction, etag).statusCode());
        String forbidden = "{\"name\":\"Otra\",\"location\":\"Mérida\",\"startDate\":\"2025-01-01\"}";
        assertEquals(400, send("PUT", "/works/" + id + "/general-information", forbidden, updatedTag).statusCode());
        String invalid = "{\"name\":\"\",\"location\":\"Mérida\"}";
        assertEquals(400, send("PUT", "/works/" + id + "/general-information", invalid, updatedTag).statusCode());
        var current = send("GET", "/works/" + id, null, null);
        assertEquals("Corregida", JSON.readTree(current.body()).path("name").asText());
        assertEquals(updatedTag, current.headers().firstValue("ETag").orElseThrow());
        assertEquals(404, send("GET", "/works/9223372036854775807", null, null).statusCode());
    }

    @Test
    void shouldAllowOnlyOneConcurrentCorrectionWithTheSameEtag() throws Exception {
        long id = createWork();
        String tag = send("GET", "/works/" + id, null, null).headers().firstValue("ETag").orElseThrow();
        var first = http.sendAsync(request("PUT", "/works/" + id + "/general-information",
                "{\"name\":\"Primera\",\"location\":\"Mérida\"}", tag), HttpResponse.BodyHandlers.ofString());
        var second = http.sendAsync(request("PUT", "/works/" + id + "/general-information",
                "{\"name\":\"Segunda\",\"location\":\"Mérida\"}", tag), HttpResponse.BodyHandlers.ofString());
        CompletableFuture.allOf(first, second).join();
        var statuses = List.of(first.join().statusCode(), second.join().statusCode()).stream().sorted().toList();
        assertEquals(List.of(200, 412), statuses);
    }

    @Test
    void shouldExposeFieldErrorsAndRejectReversedDatesWithoutSaving() throws Exception {
        long client = createClient();
        long before = jdbc.queryForObject("select count(*) from works", Long.class);
        var response = send("POST", "/works", """
                {"clientId":%d,"name":"Work","location":"Mérida","startDate":"2026-12-31","endDate":"2026-01-01"}
                """.formatted(client), null);
        assertEquals(400, response.statusCode(), response.body());
        var problem = JSON.readTree(response.body());
        assertEquals("VALIDATION_ERROR", problem.path("code").asText());
        assertTrue(problem.path("errors").has("endDate"));
        assertTrue(problem.has("suggestion"));
        assertEquals(before, jdbc.queryForObject("select count(*) from works", Long.class));
        var missing = send("POST", "/clients", "{\"name\":\"\",\"legalRepresentative\":\"\"}", null);
        assertEquals(400, missing.statusCode());
        var errors = JSON.readTree(missing.body()).path("errors");
        assertTrue(errors.has("name"));
        assertTrue(errors.has("legalRepresentative"));
        assertEquals("INVALID_REQUEST", JSON.readTree(send("POST", "/clients", "{", null).body()).path("code").asText());
    }

    @Test
    void shouldPaginateStableListsAndRejectInvalidPagination() throws Exception {
        createWork();
        createWork();
        var first = send("GET", "/clients?page=0&size=1", null, null);
        var second = send("GET", "/clients?page=1&size=1", null, null);
        assertEquals(200, first.statusCode());
        assertEquals(200, second.statusCode());
        assertEquals(1, JSON.readTree(first.body()).size());
        assertTrue(JSON.readTree(first.body()).path(0).path("id").asLong()
                < JSON.readTree(second.body()).path(0).path("id").asLong());
        var works = send("GET", "/works?page=0&size=1", null, null);
        assertEquals(200, works.statusCode());
        assertEquals(1, JSON.readTree(works.body()).size());
        assertEquals(400, send("GET", "/works?page=-1", null, null).statusCode());
        assertEquals(400, send("GET", "/clients?size=201", null, null).statusCode());
    }

    private long createClient() throws Exception {
        var response = send("POST", "/clients", "{\"name\":\"Cliente de prueba\",\"legalRepresentative\":\"Representante\"}", null);
        assertEquals(201, response.statusCode(), response.body());
        return JSON.readTree(response.body()).path("id").asLong();
    }

    private long createWork() throws Exception {
        long client = createClient();
        var response = send("POST", "/works", """
                {"clientId":%d,"name":"Work inicial","location":"Mérida","startDate":"2026-01-01","endDate":"2026-12-31"}
                """.formatted(client), null);
        assertEquals(201, response.statusCode(), response.body());
        return JSON.readTree(response.body()).path("id").asLong();
    }

    private HttpResponse<String> send(String method, String path, String body, String tag) throws Exception {
        return http.send(request(method, path, body, tag), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest request(String method, String path, String body, String tag) {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + path))
                .timeout(java.time.Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (tag != null) request.header("If-Match", tag);
        if (!method.equals("GET")) request.header(csrfHeader, csrfToken);
        return request.build();
    }
}
