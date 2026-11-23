package com.brica.client.core.http;

import com.brica.client.auth.api.LoginException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ServerSessionTest {
    private HttpServer server;
    private ExecutorService executor;
    private URI base;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicBoolean authenticated = new AtomicBoolean();
    private final AtomicBoolean csrfSeen = new AtomicBoolean();
    private String loginBody;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            String body;
            int status = 200;
            if (path.equals("/api/v1/auth/csrf")) {
                String token = authenticated.get() ? "after" : "before";
                body = "{\"headerName\":\"X-CSRF-TOKEN\",\"token\":\"" + token + "\"}";
                exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=" + token + "; Path=/; HttpOnly");
            } else if (path.equals("/api/v1/auth/login")) {
                loginBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                if (!"before".equals(exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN"))) {
                    status = 403;
                    body = "{}";
                } else {
                    authenticated.set(true);
                    body = "{\"id\":1,\"username\":\"Exact User\",\"name\":\"Nombre\",\"roleId\":1,\"roleName\":\"Consulta\",\"enabled\":true,\"permissions\":[\"WORKS_READ\"]}";
                }
            } else {
                csrfSeen.set("after".equals(exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN"))
                        && exchange.getRequestHeaders().getFirst("Cookie") != null);
                if (path.equals("/api/v1/auth/logout")) { authenticated.set(false); }
                status = authenticated.get() ? 200 : 401;
                body = "{}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach void stopServer() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void shouldShareCookiesRotateCsrfAndPreserveExactLoginValues() {
        var session = new ServerSession(base, true);
        var user = session.login(" Exact User ", "Pass+Word1").join();
        assertTrue(user.can("WORKS_READ"));
        assertFalse(user.can("WORKS_WRITE"));
        assertEquals("username=+Exact+User+&password=Pass%2BWord1", loginBody);
        assertEquals(200, session.send(HttpRequest.newBuilder(base.resolve("/api/v1/works"))
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build()).join().statusCode());
        assertTrue(csrfSeen.get());
        session.logout().join();
        assertNull(session.user());
        assertThrows(Exception.class, () -> session.send(HttpRequest.newBuilder(base.resolve("/api/v1/works"))
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build()).join());
    }

    @Test
    void shouldNotSendCredentialsOverHttpWithoutExplicitDevelopmentOptIn() {
        var session = new ServerSession(base, false);
        var failure = assertThrows(Exception.class, () -> session.login("User", "TestUser123").join());
        assertTrue(LoginException.messageFor(failure).contains("El cliente bloquea HTTP"));
        assertEquals(0, requests.get());
    }

    @Test
    void shouldReportServerHttpsRequirementBeforeSendingCredentials() {
        server.removeContext("/");
        var credentialRequests = new AtomicInteger();
        server.createContext("/", exchange -> {
            if (exchange.getRequestURI().getPath().equals("/api/v1/auth/login")) {
                credentialRequests.incrementAndGet();
            }
            byte[] body = "{\"code\":\"HTTPS_REQUIRED\",\"detail\":\"untrusted-detail\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        var session = new ServerSession(base, true);
        var failure = assertThrows(Exception.class, () -> session.login("User", "TestUser123").join());
        String message = LoginException.messageFor(failure);
        assertTrue(message.contains("El servidor exige HTTPS"), message);
        assertTrue(message.contains("BRICA_ALLOW_HTTP=true"), message);
        assertFalse(message.contains("contraseña"), message);
        assertFalse(message.contains("untrusted-detail"), message);
        assertEquals(0, credentialRequests.get());
        assertNull(session.user());
    }

    @ParameterizedTest
    @CsvSource({"401,El servidor rechazó el acceso", "403,CSRF", "404,HTTP 404", "500,HTTP 500"})
    void shouldDistinguishLoginHttpFailuresWithoutShowingResponseBody(int status, String expected) {
        server.createContext("/api/v1/auth/login", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"detail\":\"private-server-data\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        var session = new ServerSession(base, true);
        var failure = assertThrows(Exception.class, () -> session.login("User", "TestUser123").join());
        String message = LoginException.messageFor(failure);
        assertTrue(message.contains(expected), message);
        assertFalse(message.contains("private-server-data"), message);
        assertFalse(message.contains("TestUser123"), message);
        assertNull(session.user());
    }

    @Test
    void shouldNeverSendSessionToAnotherOrigin() {
        var session = new ServerSession(base, true);
        session.login("User", "TestUser123").join();
        int before = requests.get();
        assertThrows(Exception.class, () -> session.send(HttpRequest.newBuilder(
                URI.create("http://localhost:" + server.getAddress().getPort() + "/api/v1/works")).GET().build()).join());
        assertEquals(before, requests.get());
    }

    @Test
    void shouldClearSessionAndNotifyOnUnauthorizedWithoutRetryingWrites() {
        var session = new ServerSession(base, true);
        session.login("User", "TestUser123").join();
        var ended = new AtomicInteger();
        session.onSessionEnded(ended::incrementAndGet);
        authenticated.set(false);
        int before = requests.get();
        assertEquals(401, session.send(HttpRequest.newBuilder(base.resolve("/api/v1/works"))
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build()).join().statusCode());
        assertEquals(before + 1, requests.get());
        assertEquals(1, ended.get());
        assertNull(session.user());
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 401})
    void shouldIgnoreOldSessionResponseAndItsCookieAfterAnotherLogin(int oldStatus) throws Exception {
        var received = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        server.createContext("/api/v1/delayed", exchange -> {
            received.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) { return; }
                exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=obsolete; Path=/; HttpOnly");
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(oldStatus, body.length);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
            } finally { exchange.close(); }
        });
        server.createContext("/api/v1/cookie", exchange -> {
            byte[] body = String.valueOf(exchange.getRequestHeaders().getFirst("Cookie"))
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        var session = new ServerSession(base, true);
        session.login("First", "TestUser123").join();
        var ended = new AtomicInteger();
        session.onSessionEnded(ended::incrementAndGet);
        var delayed = session.send(HttpRequest.newBuilder(base.resolve("/api/v1/delayed"))
                .timeout(java.time.Duration.ofSeconds(15)).GET().build());
        try {
            assertTrue(received.await(5, TimeUnit.SECONDS));
            authenticated.set(false);
            var current = session.login("Second", "TestUser123").get(5, TimeUnit.SECONDS);
            release.countDown();
            assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> delayed.get(5, TimeUnit.SECONDS));
            assertSame(current, session.user());
            assertEquals(0, ended.get());
            String cookie = session.send(HttpRequest.newBuilder(base.resolve("/api/v1/cookie"))
                    .GET().build()).get(5, TimeUnit.SECONDS).body();
            assertTrue(cookie.contains("JSESSIONID=after"), cookie);
            assertFalse(cookie.contains("obsolete"), cookie);
        } finally { release.countDown(); }
    }
}
