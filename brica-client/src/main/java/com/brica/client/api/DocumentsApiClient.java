package com.brica.client.api;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static com.brica.client.api.DocumentDtos.*;
import com.brica.client.api.DocumentDtos.Package;

public final class DocumentsApiClient {
    private final ServerSession session;
    private final URI base;
    private final ObjectMapper json = new ObjectMapper();
    public DocumentsApiClient(URI base) { this(base, ServerSession.shared(base)); }
    public DocumentsApiClient(URI base, ServerSession session) { this.base = base; this.session = session; }

    public CompletableFuture<List<Attribute>> attributes() { return request("GET", "/document-attributes", null, null, new TypeReference<>() { }); }
    public CompletableFuture<List<Field>> fields(int page) { return request("GET", "/reference-fields?page=" + page + "&size=50", null, null, new TypeReference<>() { }); }
    public CompletableFuture<Field> createField(String key, String name, String attribute) {
        return request("POST", "/reference-fields", Map.of("key", key, "visibleName", name, "attribute", attribute), null, new TypeReference<>() { });
    }
    public CompletableFuture<Field> renameField(long id, String name) {
        return request("PUT", "/reference-fields/" + id, Map.of("visibleName", name), null, new TypeReference<>() { });
    }
    public CompletableFuture<List<Template>> templates(int page) { return request("GET", "/document-templates?page=" + page + "&size=50", null, null, new TypeReference<>() { }); }
    public CompletableFuture<Template> saveTemplate(Long id, String name, String type, TemplateState state) {
        return request(id == null ? "POST" : "PUT", "/document-templates" + (id == null ? "" : "/" + id),
                Map.of("name", name, "documentType", type, "state", state.name()), null, new TypeReference<>() { });
    }
    public CompletableFuture<Void> deleteTemplate(long id) { return request("DELETE", "/document-templates/" + id, null, null, new TypeReference<>() { }); }
    public CompletableFuture<List<Version>> versions(long id) { return request("GET", "/document-templates/" + id + "/versions", null, null, new TypeReference<>() { }); }
    public CompletableFuture<Review> analyze(long id, Path file) { return multipart("/document-templates/" + id + "/analyses", file, null, new TypeReference<>() { }); }
    public CompletableFuture<Operation> confirm(long id, Path file, UUID confirmationId, Review review, Map<Long, Boolean> required, boolean staticAccepted) {
        return multipart("/document-templates/" + id + "/versions", file, Map.of("confirmationId", confirmationId.toString(),
                "fingerprint", review.fingerprint(), "required", required, "staticAccepted", staticAccepted), new TypeReference<>() { });
    }
    public CompletableFuture<Operation> registration(String id, boolean recover) {
        UUID.fromString(id);
        return request(recover ? "POST" : "GET", "/document-registrations/" + id + (recover ? "/recover" : ""), null, null, new TypeReference<>() { });
    }
    public CompletableFuture<List<Package>> packages(int page) { return request("GET", "/document-packages?page=" + page + "&size=50", null, null, new TypeReference<>() { }); }
    public CompletableFuture<Package> documentPackage(long id) { return request("GET", "/document-packages/" + id, null, null, new TypeReference<>() { }); }
    public CompletableFuture<Package> savePackage(Package previous, Long client, String name, List<Long> ids, boolean confirm) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientId", client); body.put("name", name); body.put("templateIds", ids); body.put("confirm", confirm);
        return request(previous == null ? "POST" : "PUT", "/document-packages" + (previous == null ? "" : "/" + previous.id()),
                body, previous == null ? null : "\"" + previous.revision() + "\"", new TypeReference<>() { });
    }
    public CompletableFuture<Void> discard(Package previous) {
        return request("DELETE", "/document-packages/" + previous.id() + "/draft", null, "\"" + previous.revision() + "\"", new TypeReference<>() { });
    }
    public CompletableFuture<List<PackageOption>> options(long workId) {
        return request("GET", "/works/" + workId + "/document-options?size=100", null, null, new TypeReference<>() { });
    }
    public CompletableFuture<List<ResolvedDocument>> resolve(long workId, PackageOption option, List<TemplateOption> templates) {
        var selections = templates.stream().map(t -> Map.of("templateId", t.id(), "version", t.version())).toList();
        return request("POST", "/works/" + workId + "/document-selection/resolve",
                Map.of("packageId", option.id(), "packageRevision", option.revision(), "selections", selections), null, new TypeReference<>() { });
    }
    private <T> CompletableFuture<T> request(String method, String path, Object body, String etag, TypeReference<T> type) {
        try {
            var builder = HttpRequest.newBuilder(base.resolve("/api/v1" + path)).timeout(Duration.ofSeconds(35));
            if (etag != null) { builder.header("If-Match", etag); }
            builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
            if (body != null) { builder.header("Content-Type", "application/json"); }
            return send(builder.build(), type);
        } catch (Exception failure) { return CompletableFuture.failedFuture(failure); }
    }
    private <T> CompletableFuture<T> multipart(String path, Path file, Object metadata, TypeReference<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (Files.size(file) > 1048576) { throw new IllegalArgumentException("El DOCX debe ocupar como máximo 1 MiB en esta prueba."); }
                byte[] bytes;
                try (var in = Files.newInputStream(file)) { bytes = in.readNBytes(1048577); }
                if (bytes.length > 1048576) { throw new IllegalArgumentException("El DOCX supera el límite de prueba."); }
                String boundary = "brica-" + UUID.randomUUID();
                var output = new ByteArrayOutputStream();
                // Do not interpolate arbitrary paths or CR/LF into multipart headers.
                String filename = file.getFileName().toString().replaceAll("[\\p{Cntrl}\\\"\\\\]", "_");
                output.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename
                        + "\"\r\nContent-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                output.write(bytes); output.write("\r\n".getBytes(StandardCharsets.UTF_8));
                if (metadata != null) {
                    output.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"metadata\"\r\nContent-Type: application/json\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    output.write(json.writeValueAsBytes(metadata)); output.write("\r\n".getBytes(StandardCharsets.UTF_8));
                }
                output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                return HttpRequest.newBuilder(base.resolve("/api/v1" + path)).timeout(Duration.ofSeconds(35))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(output.toByteArray())).build();
            } catch (Exception failure) { throw new java.util.concurrent.CompletionException(failure); }
        }).thenCompose(request -> send(request, type));
    }
    private <T> CompletableFuture<T> send(HttpRequest request, TypeReference<T> type) {
        return session.send(request).thenApply(response -> {
            try {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    JsonNode problem;
                    try { problem = json.readTree(response.body()); }
                    catch (Exception malformed) { problem = json.createObjectNode(); }
                    Review updated = problem.has("updatedAnalysis") ? json.treeToValue(problem.get("updatedAnalysis"), Review.class) : null;
                    String message = problem.path("detail").asText("No se pudo completar la operación (HTTP " + response.statusCode() + ").");
                    if (problem.path("errors").isObject()) {
                        var errors = problem.path("errors").elements();
                        while (errors.hasNext()) { message += "\n" + errors.next().asText(); }
                    }
                    throw new DocumentApiException(response.statusCode(), message, updated);
                }
                return response.statusCode() == 204 ? null : json.readValue(response.body(), type);
            } catch (DocumentApiException failure) { throw failure; }
            catch (Exception failure) { throw new IllegalStateException("No se pudo interpretar la respuesta documental.", failure); }
        });
    }
    public static final class DocumentApiException extends RuntimeException {
        private final int status;
        private final Review updatedAnalysis;
        public DocumentApiException(int status, String message, Review updatedAnalysis) {
            super(message); this.status = status; this.updatedAnalysis = updatedAnalysis;
        }
        public int status() { return status; }
        public Review updatedAnalysis() { return updatedAnalysis; }
    }
}
