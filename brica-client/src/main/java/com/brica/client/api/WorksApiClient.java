package com.brica.client.api;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class WorksApiClient {

    private static final Duration TIMEOUT =
            Duration.ofSeconds(10);

    private final ServerSession session;
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI worksUri;

    public WorksApiClient(URI serverBaseUri) {

        session = ServerSession.shared(serverBaseUri);

        worksUri = serverBaseUri.resolve(
                "/api/v1/works"
        );
    }

    // LISTAR OBRAS

    public CompletableFuture<List<WorkDto>> list(
            int page,
            int size) {

        if (page < 0 || size < 1 || size > 200) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            "Paginación inválida."
                    )
            );
        }

        URI uri = URI.create(
                worksUri + "?page=" + page + "&size=" + size
        );

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .GET()
                .build();

        return send(
                request,
                response -> List.of(
                        read(
                                response.body(),
                                WorkDto[].class
                        )
                )
        );
    }

    // CONSULTAR OBRA CON SU ETAG

    public CompletableFuture<VersionedWork> get(long id) {

        HttpRequest request = HttpRequest.newBuilder(
                        workUri(id)
                )
                .timeout(TIMEOUT)
                .GET()
                .build();

        return send(request, response -> {

            String etag = response.headers()
                    .firstValue("ETag")
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "El servidor no devolvió "
                                            + "el ETag de la obra."
                            )
                    );

            WorkDto work = read(
                    response.body(),
                    WorkDto.class
            );

            return new VersionedWork(work, etag);
        });
    }

    // REGISTRAR OBRA

    public CompletableFuture<WorkDto> create(
            long clientId,
            String name,
            String location,
            String description,
            LocalDate startDate,
            LocalDate endDate) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("clientId", clientId);
        body.put("name", name);
        body.put("location", location);
        body.put("description", description);

        body.put(
                "startDate",
                startDate.toString()
        );

        body.put(
                "endDate",
                endDate == null ? null : endDate.toString()
        );

        HttpRequest request = HttpRequest.newBuilder(
                        worksUri
                )
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                toJson(body),
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        return send(
                request,
                response -> read(
                        response.body(),
                        WorkDto.class
                )
        );
    }

    // CORREGIR INFORMACIÓN GENERAL

    public CompletableFuture<WorkDto> updateGeneralInformation(
            long id,
            String etag,
            String name,
            String location,
            String description) {

        if (etag == null || etag.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException(
                            "Se requiere un ETag válido."
                    )
            );
        }

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("name", name);
        body.put("location", location);
        body.put("description", description);

        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(
                                workUri(id) + "/general-information"
                        )
                )
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("If-Match", etag)
                .PUT(
                        HttpRequest.BodyPublishers.ofString(
                                toJson(body),
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        return send(
                request,
                response -> read(
                        response.body(),
                        WorkDto.class
                )
        );
    }

    // ELIMINAR OBRA

    public CompletableFuture<Void> delete(long id) {

        HttpRequest request = HttpRequest.newBuilder(
                        workUri(id)
                )
                .timeout(TIMEOUT)
                .DELETE()
                .build();

        return send(request, response -> null);
    }

    // EJECUCIÓN HTTP Y CÓDIGOS DE RESPUESTA

    private <T> CompletableFuture<T> send(
            HttpRequest request,
            Function<HttpResponse<String>, T> parser) {

        return session.send(request).thenApply(response -> {

            int status = response.statusCode();

            if (status < 200 || status >= 300) {
                throw new ApiException(
                        status,
                        errorMessage(response.body(), status)
                );
            }

            return parser.apply(response);
        });
    }

    // JSON

    private String toJson(Object value) {

        try {
            return mapper.writeValueAsString(value);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el JSON.",
                    exception
            );
        }
    }

    private <T> T read(String json, Class<T> type) {

        try {
            return mapper.readValue(json, type);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "El servidor devolvió un JSON inválido.",
                    exception
            );
        }
    }

    // ERRORES DEL BACKEND

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
            return "El servidor devolvió HTTP " + status;
        }
    }

    private URI workUri(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID de la obra debe ser positivo."
            );
        }

        return URI.create(worksUri + "/" + id);
    }

    // RESPUESTA CON CONTROL DE VERSIÓN

    public record VersionedWork(
            WorkDto work,
            String etag
    ) {
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
