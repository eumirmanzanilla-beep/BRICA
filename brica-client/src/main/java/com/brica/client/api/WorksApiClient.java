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
                startDate == null ? null : startDate.toString()
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

    public record DocumentInformation(String contractNumber, String contractor, java.math.BigDecimal contractedAmount,
            java.math.BigDecimal amountBeforeTax, java.math.BigDecimal taxAmount, String startDate, String endDate,
            String actualEndDate, String status, Map<String, String> additionalValues) { }
    public record VersionedDocumentInformation(DocumentInformation information, String etag) { }
    public record CompleteInformation(WorkDto work, DocumentInformation documentInformation) { }
    public record VersionedInformation(CompleteInformation information, String etag) { }
    public CompletableFuture<VersionedInformation> getInformation(long id) {
        var request = HttpRequest.newBuilder(URI.create(workUri(id) + "/information")).timeout(TIMEOUT).GET().build();
        return send(request, response -> new VersionedInformation(read(response.body(), CompleteInformation.class), response.headers().firstValue("ETag").orElseThrow()));
    }
    public CompletableFuture<CompleteInformation> updateInformation(long id, String etag, String name, String location, String description, DocumentInformation info) {
        var body = new LinkedHashMap<String, Object>(); body.put("name", name); body.put("location", location);
        body.put("description", description); body.put("documentInformation", info);
        var request = HttpRequest.newBuilder(URI.create(workUri(id) + "/information")).timeout(TIMEOUT)
                .header("Content-Type", "application/json").header("If-Match", etag)
                .PUT(HttpRequest.BodyPublishers.ofString(toJson(body))).build();
        return send(request, response -> read(response.body(), CompleteInformation.class));
    }
    public record Attachment(Long id, String kind, String originalName, String mimeType, long sizeBytes, String sha256) {
        @Override public String toString() { return ("CONTRACT".equals(kind) ? "Contrato: " : "Presupuesto base: ") + originalName; }
    }
    public CompletableFuture<List<Attachment>> attachments(long workId, int page) {
        var request = HttpRequest.newBuilder(URI.create(workUri(workId) + "/attachments?page=" + page + "&size=50")).timeout(TIMEOUT).GET().build();
        return send(request, response -> List.of(read(response.body(), Attachment[].class)));
    }
    public CompletableFuture<List<Attachment>> allAttachments(long workId) {
        return allAttachments(workId, 0, new java.util.ArrayList<>());
    }
    private CompletableFuture<List<Attachment>> allAttachments(long workId, int page, List<Attachment> result) {
        return attachments(workId, page).thenCompose(items -> {
            result.addAll(items);
            return items.size() < 50 ? CompletableFuture.completedFuture(List.copyOf(result)) : allAttachments(workId, page + 1, result);
        });
    }
    public CompletableFuture<Attachment> upload(long workId, String kind, java.nio.file.Path file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes;
                try (var input = java.nio.file.Files.newInputStream(file)) { bytes = input.readNBytes(10 * 1024 * 1024 + 1); }
                if (bytes.length > 10 * 1024 * 1024) { throw new IllegalArgumentException("El archivo supera 10 MiB."); }
                String boundary = "brica-" + java.util.UUID.randomUUID();
                var body = new java.io.ByteArrayOutputStream();
                String filename = file.getFileName().toString().replaceAll("[\\p{Cntrl}\\\"\\\\]", "_");
                body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"kind\"\r\n\r\n" + kind + "\r\n").getBytes(StandardCharsets.UTF_8));
                body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                body.write(bytes); body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                return HttpRequest.newBuilder(URI.create(workUri(workId) + "/attachments")).timeout(Duration.ofSeconds(35))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
            } catch (java.io.IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
        }).thenCompose(request -> send(request, response -> read(response.body(), Attachment.class)));
    }
    public CompletableFuture<Void> download(long workId, long attachmentId, java.nio.file.Path destination) {
        var request = HttpRequest.newBuilder(URI.create(workUri(workId) + "/attachments/" + attachmentId + "/file")).timeout(Duration.ofSeconds(35)).GET().build();
        return session.sendBytes(request).thenAccept(response -> {
            if (response.statusCode() != 200) { throw new ApiException(response.statusCode(), "No se pudo descargar el adjunto."); }
            try { java.nio.file.Files.write(destination, response.body()); }
            catch (java.io.IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
        });
    }

    public CompletableFuture<VersionedDocumentInformation> getDocumentInformation(long id) {
        var request = HttpRequest.newBuilder(URI.create(workUri(id) + "/document-information")).timeout(TIMEOUT).GET().build();
        return send(request, response -> new VersionedDocumentInformation(
                read(response.body(), DocumentInformation.class), response.headers().firstValue("ETag").orElseThrow()));
    }

    public CompletableFuture<Void> updateDocumentInformation(long id, String etag, DocumentInformation info) {
        var request = HttpRequest.newBuilder(URI.create(workUri(id) + "/document-information"))
                .timeout(TIMEOUT).header("Content-Type", "application/json").header("If-Match", etag)
                .PUT(HttpRequest.BodyPublishers.ofString(toJson(info), StandardCharsets.UTF_8)).build();
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
