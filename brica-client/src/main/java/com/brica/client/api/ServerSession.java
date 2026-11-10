package com.brica.client.api;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.Map;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.brica.client.config.ClientConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ServerSession {
    private static final ConcurrentHashMap<URI, ServerSession> SESSIONS = new ConcurrentHashMap<>();
    private final URI base;
    private final boolean allowHttp;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();
    private final AtomicLong generation = new AtomicLong();
    private volatile Csrf csrf;
    private volatile SessionUser user;
    private volatile Runnable onSessionEnded = () -> { };

    public static ServerSession shared(URI base) {
        return SESSIONS.computeIfAbsent(base, uri -> new ServerSession(uri, ClientConfig.allowInsecureHttp()));
    }

    public ServerSession(URI base, boolean allowHttp) {
        this.base = base;
        this.allowHttp = allowHttp;
        this.http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(3)).build();
    }

    public void onSessionEnded(Runnable action) { onSessionEnded = action; }
    public SessionUser user() { return user; }

    public CompletableFuture<SessionUser> login(String username, String password) {
        clear();
        long epoch = generation.get();
        return fetchCsrf().thenCompose(ignored -> {
            String body = "username=" + encode(username) + "&password=" + encode(password);
            return send(HttpRequest.newBuilder(base.resolve("/api/v1/auth/login"))
                    .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build());
        }).thenCompose(response -> {
            if (response.statusCode() != 200) {
                return CompletableFuture.failedFuture(loginFailure(response));
            }
            SessionUser authenticated = read(response.body(), SessionUser.class);
            return fetchCsrf().thenApply(ignored -> {
                if (epoch != generation.get()) { throw new IllegalStateException("La sesión cambió durante el acceso."); }
                user = authenticated;
                return authenticated;
            });
        }).whenComplete((result, failure) -> { if (failure != null && epoch == generation.get()) { clear(); } });
    }

    private CompletableFuture<Void> fetchCsrf() {
        long epoch = generation.get();
        return raw(HttpRequest.newBuilder(base.resolve("/api/v1/auth/csrf"))
                .timeout(Duration.ofSeconds(10)).GET().build()).thenAccept(response -> {
            if (epoch != generation.get()) { throw new IllegalStateException("La sesión cambió durante la consulta."); }
            if (response.statusCode() != 200) {
                throw loginFailure(response);
            }
            csrf = read(response.body(), Csrf.class);
        });
    }

    public CompletableFuture<HttpResponse<String>> send(HttpRequest request) {
        long epoch = generation.get();
        boolean mutation = !Set.of("GET", "HEAD", "OPTIONS").contains(request.method());
        if (mutation) {
            Csrf token = csrf;
            if (token == null) { return CompletableFuture.failedFuture(new IllegalStateException("Inicia sesión antes de guardar cambios.")); }
            request = HttpRequest.newBuilder(request, (name, value) -> !name.equalsIgnoreCase(token.headerName()))
                    .header(token.headerName(), token.token()).build();
        }
        return raw(request).thenApply(response -> {
            if (epoch != generation.get()) { throw new IllegalStateException("La sesión cambió; vuelve a consultar."); }
            if (response.statusCode() == 401) {
                boolean wasAuthenticated = user != null;
                clear();
                if (wasAuthenticated) { onSessionEnded.run(); }
            }
            return response;
        });
    }

    public CompletableFuture<Void> logout() {
        if (csrf == null) { clear(); return CompletableFuture.completedFuture(null); }
        return send(HttpRequest.newBuilder(base.resolve("/api/v1/auth/logout"))
                .timeout(Duration.ofSeconds(3)).POST(HttpRequest.BodyPublishers.noBody()).build())
                .handle((response, failure) -> { clear(); return null; });
    }

    private CompletableFuture<HttpResponse<String>> raw(HttpRequest request) {
        URI uri = request.uri();
        if (!base.getScheme().equalsIgnoreCase(uri.getScheme()) || !base.getAuthority().equals(uri.getAuthority())) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Destino ajeno a la sesión del servidor."));
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !(allowHttp && "http".equalsIgnoreCase(uri.getScheme()))) {
            return CompletableFuture.failedFuture(new LoginException(
                    "El cliente bloquea HTTP. Usa HTTPS o, solo en desarrollo local, inicia el cliente con BRICA_ALLOW_HTTP=true."));
        }
        long epoch;
        HttpRequest outgoing;
        try {
            synchronized (this) {
                epoch = generation.get();
                var builder = HttpRequest.newBuilder(request, (name, value) -> !name.equalsIgnoreCase("Cookie"));
                cookies.get(uri, Map.of()).forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
                outgoing = builder.build();
            }
        } catch (IOException failure) {
            return CompletableFuture.failedFuture(new IllegalStateException("No se pudo preparar la sesión."));
        }
        return http.sendAsync(outgoing, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(response -> {
            synchronized (this) {
                if (epoch == generation.get()) {
                    try { cookies.put(uri, response.headers().map()); }
                    catch (IOException failure) { throw new IllegalStateException("No se pudo actualizar la sesión."); }
                }
            }
            return response;
        });
    }

    public synchronized void clear() {
        generation.incrementAndGet();
        cookies.getCookieStore().removeAll();
        csrf = null;
        user = null;
    }

    private <T> T read(String body, Class<T> type) {
        try { return json.readValue(body, type); }
        catch (Exception failure) { throw new LoginException("El servidor devolvió una respuesta de sesión no válida. Revisa que cliente y servidor estén actualizados."); }
    }

    private LoginException loginFailure(HttpResponse<String> response) {
        String code = "";
        try { code = json.readTree(response.body()).path("code").asText(); }
        catch (Exception ignored) { /* No mostrar cuerpos de error ni detalles no confiables. */ }
        if ("HTTPS_REQUIRED".equals(code)) {
            return new LoginException("El servidor exige HTTPS. Usa una URL HTTPS o, solo en desarrollo local, inicia también el servidor con BRICA_ALLOW_HTTP=true.");
        }
        return new LoginException(switch (response.statusCode()) {
            case 401 -> "El servidor rechazó el acceso. Revisa el usuario exacto, la contraseña o el estado de la cuenta.";
            case 403 -> "El servidor rechazó la protección de la sesión (CSRF). Vuelve a iniciar sesión.";
            case 404 -> "El servidor no dispone del acceso requerido (HTTP 404). Actualiza el backend y revisa BRICA_SERVER_URL.";
            default -> "El servidor no pudo completar el acceso (HTTP " + response.statusCode() + "). Vuelve a intentar o revisa el servidor.";
        });
    }
    private static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private record Csrf(String headerName, String token) { }
    public record SessionUser(Long id, String username, String name, Long roleId, String roleName,
            boolean enabled, Set<String> permissions) {
        public boolean can(String permission) { return permissions.contains(permission); }
    }
}
