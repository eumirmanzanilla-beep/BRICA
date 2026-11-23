package com.brica.client.clients.api;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.brica.client.core.http.ServerSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ClientsApiClient {

    private static final Duration REQUEST_TIMEOUT =
            Duration.ofSeconds(10);

    private final ServerSession session;
    private final ObjectMapper mapper;
    private final URI clientsUri;
    private final java.util.concurrent.ConcurrentHashMap<Long, String> revisions = new java.util.concurrent.ConcurrentHashMap<>();

    public ClientsApiClient(URI serverBaseUri) {

        session = ServerSession.shared(serverBaseUri);

        mapper = new ObjectMapper();

        clientsUri = serverBaseUri.resolve(
                "/api/v1/clients"
        );
    }

    // CONSULTAR CLIENTES

    public CompletableFuture<List<ClientDto>> list(
            int page,
            int size) {

        if (page < 0 || size < 1) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            "Paginación inválida."
                    )
            );
        }

        URI uri = URI.create(
                clientsUri + "?page=" + page + "&size=" + size
        );

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        return send(
                request,
                body -> List.of(
                        mapper.readValue(
                                body,
                                ClientDto[].class
                        )
                )
        );
    }

    // CONSULTAR UN CLIENTE

    public CompletableFuture<ClientDto> get(long id) {

        HttpRequest request = HttpRequest.newBuilder(
                        clientUri(id)
                )
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        return send(
                request,
                body -> mapper.readValue(
                        body,
                        ClientDto.class
                )
        );
    }

    // REGISTRAR CLIENTE

    public CompletableFuture<ClientDto> create(
            String name,
            String legalRepresentative) {

        return save(
                "POST",
                clientsUri,
                name,
                legalRepresentative
        );
    }

    // ACTUALIZAR CLIENTE

    public CompletableFuture<ClientDto> update(
            long id,
            String expectedRevision,
            String name,
            String legalRepresentative) {

        if (expectedRevision == null) { return CompletableFuture.failedFuture(new IllegalStateException("Consulta el cliente antes de corregirlo.")); }
        return save(
                "PUT",
                clientUri(id),
                name,
                legalRepresentative,
                expectedRevision
        );
    }

    // ELIMINAR CLIENTE

    public CompletableFuture<Void> delete(long id, String expectedRevision) {

        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        clientUri(id)
                )
                .timeout(REQUEST_TIMEOUT)
                .DELETE();
        String revision = expectedRevision;
        if (revision == null) { return CompletableFuture.failedFuture(new IllegalStateException("Vuelve a consultar el cliente antes de eliminar.")); }
        HttpRequest request = builder.header("If-Match", revision).build();

        return send(request, body -> null);
    }

    // OPERACIÓN COMPARTIDA DE REGISTRO Y EDICIÓN

    private CompletableFuture<ClientDto> save(
            String method,
            URI uri,
            String name,
            String legalRepresentative) {
        return save(method, uri, name, legalRepresentative, null);
    }

    private CompletableFuture<ClientDto> save(String method, URI uri, String name, String legalRepresentative, String expectedRevision) {

        final String json;

        try {
            json = mapper.writeValueAsString(
                    Map.of(
                            "name", name,
                            "legalRepresentative",
                            legalRepresentative
                    )
            );

        } catch (JsonProcessingException exception) {

            return CompletableFuture.failedFuture(
                    exception
            );
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header(
                        "Content-Type",
                        "application/json"
                )
                .method(
                        method,
                        HttpRequest.BodyPublishers.ofString(
                                json,
                                StandardCharsets.UTF_8
                        )
                );
        if (method.equals("PUT")) {
            String revision = expectedRevision;
            if (revision == null) { return CompletableFuture.failedFuture(new IllegalStateException("Vuelve a consultar el cliente antes de guardar.")); }
            builder.header("If-Match", revision);
        }
        HttpRequest request = builder.build();

        return send(
                request,
                body -> mapper.readValue(
                        body,
                        ClientDto.class
                )
        );
    }

    // EJECUCIÓN ASÍNCRONA DE SOLICITUDES HTTP

    private <T> CompletableFuture<T> send(
            HttpRequest request,
            ResponseParser<T> parser) {

        return session.send(request).thenApply(response -> {

            int status = response.statusCode();

            if (status < 200 || status >= 300) {
                throw new ApiException(
                        status,
                        errorMessage(
                                response.body(),
                                status
                        )
                );
            }

            try {
                T parsed = parser.parse(response.body());
                if (parsed instanceof ClientDto client && client.revision() != null) { revisions.put(client.id(), client.revision()); }
                if (parsed instanceof List<?> clients) {
                    clients.forEach(value -> { if (value instanceof ClientDto client && client.revision() != null) { revisions.put(client.id(), client.revision()); } });
                }
                return parsed;

            } catch (JsonProcessingException exception) {

                throw new IllegalStateException(
                        "La respuesta del servidor "
                                + "contiene JSON inválido.",
                        exception
                );
            }
        });
    }

    // INTERPRETAR ERRORES DE SPRING BOOT

    private String errorMessage(
            String body,
            int status) {

        try {
            JsonNode problem = mapper.readTree(body);

            if (problem == null) {
                return "Error HTTP " + status;
            }

            String title = problem.path("title")
                    .asText("Error HTTP " + status);

            String detail = problem.path("detail")
                    .asText("");

            String suggestion = problem.path("suggestion")
                    .asText("");

            StringBuilder message = new StringBuilder(title);

            if (!detail.isBlank()) {
                message.append("\n").append(detail);
            }

            JsonNode errors = problem.path("errors");

            if (errors.isObject()) {
                errors.fields().forEachRemaining(entry ->
                        message.append("\n")
                                .append(entry.getKey())
                                .append(": ")
                                .append(entry.getValue().asText())
                );
            }

            if (!suggestion.isBlank()) {
                message.append("\n").append(suggestion);
            }

            return message.toString();

        } catch (JsonProcessingException exception) {

            return "El servidor devolvió un error HTTP "
                    + status + ".";
        }
    }

    private URI clientUri(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID del cliente debe ser positivo."
            );
        }

        return URI.create(clientsUri + "/" + id);
    }

    @FunctionalInterface
    private interface ResponseParser<T> {
        T parse(String body) throws JsonProcessingException;
    }

    public static final class ApiException
            extends RuntimeException {

        private final int status;

        public ApiException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }
}
