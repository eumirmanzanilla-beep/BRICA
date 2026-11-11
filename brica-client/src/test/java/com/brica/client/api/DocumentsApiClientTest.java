package com.brica.client.api;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class DocumentsApiClientTest {
    HttpServer server;
    ExecutorService executor;
    DocumentsApiClient api;
    AtomicReference<String> requestBody = new AtomicReference<>();
    AtomicReference<String> ifMatch = new AtomicReference<>();
    AtomicInteger calls = new AtomicInteger();
    @TempDir Path directory;
    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor(); server.setExecutor(executor);
        server.createContext("/", e -> {
            String path = e.getRequestURI().getPath(); String response;
            if (path.endsWith("/csrf")) { response = "{\"headerName\":\"X-CSRF-TOKEN\",\"token\":\"token\"}"; }
            else if (path.endsWith("/login")) {
                response = "{\"id\":1,\"username\":\"User\",\"name\":\"User\",\"roleId\":1,\"roleName\":\"Admin\",\"enabled\":true,\"permissions\":[\"DOCUMENT_CONFIG_ADMIN\"]}";
            } else {
                calls.incrementAndGet(); requestBody.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                ifMatch.set(e.getRequestHeaders().getFirst("If-Match"));
                if (!e.getRequestMethod().equals("GET")) { assertEquals("token", e.getRequestHeaders().getFirst("X-CSRF-TOKEN")); }
                response = "{\"id\":7,\"clientId\":2,\"name\":\" Exact Name \",\"state\":\"DRAFT\",\"revision\":4,\"templates\":[]}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8); e.sendResponseHeaders(200, bytes.length); e.getResponseBody().write(bytes); e.close();
        });
        server.start();
        var base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        var session = new ServerSession(base, true); session.login("User", "TestUser123").join(); api = new DocumentsApiClient(base, session);
    }
    @AfterEach void stop() { server.stop(0); executor.shutdownNow(); }
    @Test void shouldPreserveExactNamesNullDraftValuesAndSendRevision() throws Exception {
        var draft = api.savePackage(null, null, " Exact Name ", List.of(), false).join();
        var body = new ObjectMapper().readTree(requestBody.get());
        assertTrue(body.get("clientId").isNull()); assertEquals(" Exact Name ", body.get("name").asText());
        api.savePackage(draft, 2L, draft.name(), List.of(1L), false).join();
        assertEquals("\"4\"", ifMatch.get()); assertEquals(2, calls.get());
    }
    @Test void shouldReturnUpdatedAnalysisWithoutAutomaticallyConfirmingOrRetrying() {
        server.createContext("/api/v1/document-templates/1/versions", e -> {
            calls.incrementAndGet(); requestBody.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = "{\"detail\":\"Revisa de nuevo\",\"updatedAnalysis\":{\"fingerprint\":\"new\",\"fields\":[],\"occurrences\":[],\"diagnostics\":[],\"staticDocument\":true}}".getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(409, bytes.length); e.getResponseBody().write(bytes); e.close();
        });
        try {
            var file = directory.resolve("test.docx"); Files.write(file, new byte[]{1, 2, 3});
            var review = new DocumentDtos.Review("old", List.of(), List.of(), List.of(), true);
            var key = UUID.randomUUID();
            var failed = assertThrows(CompletionException.class, () -> api.confirm(1, file, key, review, Map.of(), true).join());
            var problem = assertInstanceOf(DocumentsApiClient.DocumentApiException.class, failed.getCause());
            assertEquals(409, problem.status()); assertEquals("new", problem.updatedAnalysis().fingerprint());
            assertEquals(1, calls.get());
            assertTrue(requestBody.get().contains(key.toString())); assertTrue(requestBody.get().contains("name=\"metadata\""));
        } catch (java.io.IOException e) { fail(e); }
    }
    @Test void shouldRejectOversizedUploadsBeforeSendingThem() throws Exception {
        var file = directory.resolve("too-large.docx"); Files.write(file, new byte[1048577]);
        assertThrows(CompletionException.class, () -> api.analyze(1, file).join()); assertEquals(0, calls.get());
    }
}
