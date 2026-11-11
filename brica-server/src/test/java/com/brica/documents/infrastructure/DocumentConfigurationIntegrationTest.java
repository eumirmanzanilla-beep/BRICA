package com.brica.documents.infrastructure;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import com.brica.documents.application.*;
import com.brica.documents.application.port.DocumentConfigurationStore;
import com.brica.documents.application.port.DocumentFileStore;
import com.brica.documents.domain.*;
import com.brica.server.security.SecurityTestData;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "brica.security.allow-insecure-http=true")
class DocumentConfigurationIntegrationTest {
    @Container @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");
    static final Path STORAGE = Path.of("/tmp/opencode/brica-document-test-" + UUID.randomUUID());
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("brica.documents.storage-root", STORAGE::toString);
    }
    @Autowired TemplateManagementService templates;
    @Autowired TemplateAnalysisService analyses;
    @Autowired RegisterTemplateVersionService versions;
    @Autowired DocumentPackageService packages;
    @Autowired DocumentSelectionService selection;
    @Autowired RegisterReferenceFieldService registerFields;
    @Autowired UpdateReferenceFieldService updateFields;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @MockitoSpyBean DocumentFileStore files;
    @MockitoSpyBean DocumentConfigurationStore store;
    @LocalServerPort int port;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @BeforeEach void account() {
        SecurityTestData.installAdmin(jdbc, passwords);
        as(() -> {
            for (var entry : Map.of("NOMBRE_OBRA", ReferenceAttribute.WORK_NAME, "NOMBRE_CLIENTE", ReferenceAttribute.CLIENT_NAME).entrySet()) {
                if (jdbc.queryForObject("select count(*) from reference_fields where key = ?", Long.class, entry.getKey()) == 0) {
                    registerFields.register(entry.getKey(), entry.getKey(), entry.getValue());
                }
            }
            return null;
        });
    }
    byte[] fixture(String name) {
        try (var in = getClass().getResourceAsStream("/documents/" + name + ".docx")) { return in.readAllBytes(); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    long template(String type) { return as(() -> templates.create("Formato " + UUID.randomUUID(), type, TemplateState.CURRENT).id()); }
    TemplateAnalysisService.Review review(long id) { return as(() -> analyses.analyze(id, fixture("acta-inicio-prototipo"))); }
    Map<Long, Boolean> required(TemplateAnalysisService.Review review) {
        var result = new TreeMap<Long, Boolean>(); review.fields().forEach(f -> result.put(f.id(), true)); return result;
    }
    RegisterTemplateVersionService.OperationView register(long id) {
        var r = review(id);
        return as(() -> versions.confirm(id, UUID.randomUUID(), "test.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), required(r), false).operation());
    }
    long client() { return jdbc.queryForObject("insert into cliente(nombre, representante_legal) values ('Cliente de prueba', 'Representante') returning id_cliente", Long.class); }
    long work(long client) { return jdbc.queryForObject("insert into works(client_id, name, location, start_date) values (?, 'Obra de prueba', 'Mérida', '2026-01-01') returning id_work", Long.class, client); }
    <T> T as(Supplier<T> action) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("IntegrationAdmin", null, List.of()));
        SecurityContextHolder.setContext(context);
        try { return action.get(); } finally { SecurityContextHolder.clearContext(); }
    }
    @Test void shouldAnalyzeAllRegionsRejectUnknownAndDemandCompleteClassification() {
        long id = template("ACTA");
        var r = review(id);
        assertEquals(4, r.occurrences().size()); assertEquals(2, r.fields().size()); assertTrue(r.valid());
        assertTrue(r.fields().stream().allMatch(f -> f.proposedRequired() == null));
        var unknown = as(() -> analyses.analyze(id, fixture("marcador-desconocido")));
        assertEquals("UNKNOWN_MARKER", unknown.diagnostics().getFirst().code());
        assertThrows(com.brica.server.shared.validation.domain.InvalidDataException.class, () -> as(() -> versions.confirm(id,
                UUID.randomUUID(), "a.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), Map.of(), false)));
        assertTrue(as(() -> templates.versions(id)).isEmpty());
    }
    @Test void shouldRegisterImmutableVersionsReplayOriginalResponseAndProtectLinkedFields() {
        long id = template("ACTA");
        var r = review(id); var key = UUID.randomUUID();
        var first = as(() -> versions.confirm(id, key, "same.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), required(r), false).operation());
        assertEquals(RegistrationOperation.Status.COMPLETED, first.status()); assertEquals(1, first.versionNumber());
        var replay = as(() -> versions.confirm(id, key, "same.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), required(r), false).operation());
        assertEquals(first, replay);
        assertThrows(DocumentException.class, () -> as(() -> versions.confirm(id, key, "other.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), required(r), false)));
        assertTrue(review(id).fields().stream().allMatch(f -> Boolean.TRUE.equals(f.proposedRequired())));
        assertEquals(2, register(id).versionNumber());
        assertArrayEquals(fixture("acta-inicio-prototipo"), as(() -> templates.download(id, 1)));
        Long field = r.fields().getFirst().id();
        as(() -> updateFields.rename(field, "Etiqueta editable"));
        assertEquals(2, as(() -> templates.versions(id)).size());
        assertEquals(2L, jdbc.queryForObject("select count(*) from template_version_fields where field_id = ? and template_id = ?", Long.class, field, id));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> jdbc.update("delete from reference_fields where id_reference_field = ?", field));
    }
    @Test void shouldRequireNewReviewOnContentOrCatalogueChangeAndExplicitStaticAcceptance() {
        long id = template("STATIC");
        var r = review(id);
        var changed = as(() -> versions.confirm(id, UUID.randomUUID(), "same.docx", fixture("documento-estatico"), r.fingerprint(), required(r), false));
        assertNotNull(changed.updatedAnalysis()); assertNull(changed.operation());
        as(() -> updateFields.rename(r.fields().getFirst().id(), "Nueva etiqueta " + UUID.randomUUID().toString().substring(0, 8)));
        assertNotNull(as(() -> versions.confirm(id, UUID.randomUUID(), "same.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), required(r), false)).updatedAnalysis());
        var staticReview = as(() -> analyses.analyze(id, fixture("documento-estatico")));
        assertTrue(staticReview.staticDocument());
        assertThrows(com.brica.server.shared.validation.domain.InvalidDataException.class, () -> as(() -> versions.confirm(id,
                UUID.randomUUID(), "static.docx", fixture("documento-estatico"), staticReview.fingerprint(), Map.of(), false)));
        var registered = as(() -> versions.confirm(id, UUID.randomUUID(), "static.docx", fixture("documento-estatico"), staticReview.fingerprint(), Map.of(), true));
        assertEquals(1, registered.operation().versionNumber());
        assertTrue(as(() -> templates.version(id, 1)).fields().isEmpty());
        assertTrue(as(() -> templates.version(id, 1)).staticDocument());
    }
    @Test void shouldInheritOnlyConservedFieldsClassifyNewOnesAndPreservePreviousRequiredness() throws Exception {
        long id = template("REQUIREDNESS"); var first = review(id); var originalRequired = required(first);
        var workField = first.fields().stream().filter(f -> f.key().equals("NOMBRE_OBRA")).findFirst().orElseThrow();
        var clientField = first.fields().stream().filter(f -> f.key().equals("NOMBRE_CLIENTE")).findFirst().orElseThrow();
        originalRequired.put(clientField.id(), false);
        as(() -> versions.confirm(id, UUID.randomUUID(), "one.docx", fixture("acta-inicio-prototipo"), first.fingerprint(), originalRequired, false));
        String newKey = "NUEVO_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        var added = as(() -> registerFields.register(newKey, "Otro nombre de obra", ReferenceAttribute.WORK_NAME));
        var output = new java.io.ByteArrayOutputStream();
        try (var input = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fixture("acta-inicio-prototipo")));
                var zip = new java.util.zip.ZipOutputStream(output)) {
            java.util.zip.ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] bytes = input.readAllBytes();
                if (entry.getName().endsWith(".xml")) {
                    String replacement = entry.getName().equals("word/header1.xml") ? "{{" + newKey + "}}" : "Texto fijo";
                    bytes = new String(bytes, StandardCharsets.UTF_8).replace("{{NOMBRE_CLIENTE}}", replacement).getBytes(StandardCharsets.UTF_8);
                }
                zip.putNextEntry(new java.util.zip.ZipEntry(entry.getName())); zip.write(bytes); zip.closeEntry();
            }
        }
        byte[] second = output.toByteArray();
        var next = as(() -> analyses.analyze(id, second));
        assertTrue(next.valid()); assertEquals(2, next.fields().size());
        assertTrue(next.fields().stream().noneMatch(f -> f.id().equals(clientField.id())));
        assertEquals(Boolean.TRUE, next.fields().stream().filter(f -> f.id().equals(workField.id())).findFirst().orElseThrow().proposedRequired());
        assertNull(next.fields().stream().filter(f -> f.id().equals(added.getId())).findFirst().orElseThrow().proposedRequired());
        var requirements = Map.of(workField.id(), false, added.getId(), false);
        var registered = as(() -> versions.confirm(id, UUID.randomUUID(), "two.docx", second, next.fingerprint(), requirements, false));
        assertEquals(2, registered.operation().versionNumber());
        assertTrue(as(() -> templates.version(id, 1)).fields().stream().filter(f -> f.id().equals(workField.id())).findFirst().orElseThrow().required());
        assertTrue(as(() -> templates.version(id, 2)).fields().stream().noneMatch(TemplateManagementService.VersionField::required));
    }
    @Test void shouldRecoverAfterFilePublicationAndRollbackPartialMetadataWithoutDuplicates() {
        long id = template("RECOVERY"); var r = review(id); var key = UUID.randomUUID();
        doAnswer(call -> { call.callRealMethod(); throw new IllegalStateException("simulated process termination"); }).when(files).put(eq(key), any());
        assertThrows(IllegalStateException.class, () -> as(() -> versions.confirm(id, key, "a.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), required(r), false)));
        reset(files);
        assertEquals(RegistrationOperation.Status.PENDING, as(() -> versions.status(key)).status());
        assertTrue(as(() -> templates.versions(id)).isEmpty());
        doAnswer(call -> { if (call.getArgument(0) instanceof TemplateVersion) { throw new IllegalStateException("metadata failure"); } return call.callRealMethod(); }).when(store).add(any());
        assertEquals("REGISTRATION_PENDING", assertThrows(DocumentException.class, () -> as(() -> versions.recover(key))).code());
        reset(store);
        assertEquals(0L, jdbc.queryForObject("select count(*) from document_files where relative_path = ?", Long.class, files.relativePath(key)));
        var result = as(() -> versions.recover(key));
        assertEquals(RegistrationOperation.Status.COMPLETED, result.status());
        assertEquals(result, as(() -> versions.recover(key)));
        assertEquals(1, as(() -> templates.versions(id)).size());
    }
    @Test void shouldFailUnverifiablePendingRegistrationAndKeepLastGoodVersion() {
        long id = template("RECOVERY"); register(id); var r = review(id); var key = UUID.randomUUID();
        doThrow(new IllegalStateException("interrupted before file write")).when(files).put(eq(key), any());
        assertThrows(IllegalStateException.class, () -> as(() -> versions.confirm(id, key, "a.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), required(r), false)));
        reset(files);
        assertEquals(RegistrationOperation.Status.FAILED, as(() -> versions.recover(key)).status());
        assertEquals(1, as(() -> templates.versions(id)).size());
        assertArrayEquals(fixture("acta-inicio-prototipo"), as(() -> templates.download(id, 1)));
    }
    @Test void shouldKeepRegistrationFailedOnStorageErrorAndExcludeCorruptedFiles() throws Exception {
        long id = template("STORAGE"); register(id); var r = review(id); var key = UUID.randomUUID();
        doThrow(new DocumentException(503, "DOCUMENT_STORAGE_UNAVAILABLE", "simulated failure")).when(files).put(eq(key), any());
        var failed = as(() -> versions.confirm(id, key, "a.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), required(r), false));
        reset(files);
        assertEquals(RegistrationOperation.Status.FAILED, failed.operation().status());
        assertEquals(1, as(() -> templates.versions(id)).size());
        long client = client(); long work = work(client);
        var p = as(() -> packages.create(client, "Integrity", List.of(id), true));
        assertEquals(1, as(() -> selection.options(work, 0, 50)).getFirst().templates().size());
        var path = jdbc.queryForObject("select f.relative_path from document_files f join template_versions v on v.file_id = f.id_file where v.template_id = ?", String.class, id);
        Files.write(STORAGE.resolve(path), new byte[]{1, 2, 3});
        assertTrue(as(() -> selection.options(work, 0, 50)).isEmpty());
        assertThrows(DocumentException.class, () -> as(() -> templates.download(id, 1)));
        assertThrows(DocumentException.class, () -> as(() -> selection.resolve(work, p.id(), p.revision(), List.of(new DocumentSelectionService.Selection(id, 1)))));
    }
    @Test void shouldSerializeConcurrentDistinctRegistrationsAndReplays() throws Exception {
        long id = template("CONCURRENT"); register(id); var r = review(id);
        var sharedKey = UUID.randomUUID();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new CountDownLatch(1);
            var tasks = new ArrayList<Future<RegisterTemplateVersionService.OperationView>>();
            for (int i = 0; i < 3; i++) {
                final UUID key = i < 2 ? sharedKey : UUID.randomUUID();
                tasks.add(executor.submit(() -> { start.await(); return as(() -> versions.confirm(id, key, "a.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), required(r), false).operation()); }));
            }
            start.countDown();
            var a = tasks.get(0).get(40, TimeUnit.SECONDS); var b = tasks.get(1).get(40, TimeUnit.SECONDS);
            var c = tasks.get(2).get(40, TimeUnit.SECONDS);
            assertEquals(a.id(), b.id());
            var completed = as(() -> versions.status(sharedKey));
            assertEquals(RegistrationOperation.Status.COMPLETED, completed.status());
            assertNotEquals(completed.versionNumber(), c.versionNumber());
            assertEquals(3, as(() -> templates.versions(id)).size());
        }
    }
    @Test void shouldKeepDraftsAndUnavailableVisibleButIneligibleAndDeletedReadOnly() {
        long id = template("STATUS"); register(id);
        for (var state : List.of(TemplateState.DRAFT, TemplateState.UNAVAILABLE)) {
            as(() -> templates.update(id, "Visible", "STATUS", state));
            assertFalse(as(() -> templates.list(0, 100)).stream().noneMatch(t -> t.id().equals(id)));
            assertFalse(as(() -> templates.versions(id)).isEmpty());
            assertEquals(RegistrationOperation.Status.COMPLETED, register(id).status());
            assertThrows(DocumentException.class, () -> as(() -> packages.create(client(), "Invalid", List.of(id), true)));
            assertEquals(DocumentPackage.State.DRAFT, as(() -> packages.create(null, "Draft", List.of(id), false)).state());
        }
        as(() -> { templates.delete(id); return null; });
        assertArrayEquals(fixture("acta-inicio-prototipo"), as(() -> templates.download(id, 1)));
        assertThrows(DocumentRuleException.class, () -> as(() -> templates.update(id, "Edit", "STATUS", TemplateState.CURRENT)));
        assertThrows(DocumentRuleException.class, () -> review(id));
    }
    @Test void shouldPersistDraftsUseExactNamesRejectStaleEditsAndKeepClientFixed() {
        long id = template("PACKAGE"); register(id); long client = client();
        var draft = as(() -> packages.create(null, null, List.of(id), false));
        var reopened = as(() -> packages.get(draft.id())); assertEquals(draft, reopened);
        var confirmed = as(() -> packages.update(draft.id(), DocumentPackageService.etag(draft.revision()), client, "Paquete", List.of(id), true));
        assertEquals(DocumentPackage.State.CONFIRMED, confirmed.state());
        assertThrows(DocumentException.class, () -> as(() -> packages.update(draft.id(), DocumentPackageService.etag(draft.revision()), client, "Lost", List.of(id), true)));
        assertThrows(DocumentRuleException.class, () -> as(() -> packages.update(draft.id(), DocumentPackageService.etag(confirmed.revision()), client(), "Change", List.of(id), true)));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> as(() -> packages.create(client, "Paquete", List.of(id), true)));
        assertNotNull(as(() -> packages.create(client, "paquete", List.of(id), true)));
        assertNotNull(as(() -> packages.create(client, " Paquete ", List.of(id), true)));
        var discarded = as(() -> packages.create(null, "Discard", List.of(id), false));
        as(() -> { packages.discard(discarded.id(), DocumentPackageService.etag(discarded.revision())); return null; });
        assertThrows(DocumentException.class, () -> as(() -> packages.get(discarded.id())));
        assertEquals(1, as(() -> templates.versions(id)).size());
    }
    @Test void shouldResolveOnlyVersionFieldsAndRejectWrongClientRevisionTypeOrObsoleteVersion() {
        long a = template("SAME_TYPE"); long b = template("SAME_TYPE"); register(a); register(b);
        long client = client(); long work = work(client);
        var p = as(() -> packages.create(client, "Applicable", List.of(b, a), true));
        as(() -> packages.create(client(), "Foreign", List.of(a), true));
        as(() -> packages.create(client, "Draft", List.of(a), false));
        var options = as(() -> selection.options(work, 0, 50));
        assertEquals(1, options.size()); assertEquals(2, options.getFirst().templates().size());
        var result = as(() -> selection.resolve(work, p.id(), p.revision(), List.of(new DocumentSelectionService.Selection(a, 1))));
        assertEquals(2, result.getFirst().fields().size());
        assertTrue(result.getFirst().fields().stream().allMatch(f -> f.status() == ResolutionStatus.PRESENT && f.required()));
        assertThrows(DocumentException.class, () -> as(() -> selection.resolve(work, p.id(), p.revision(), List.of(new DocumentSelectionService.Selection(a, 1), new DocumentSelectionService.Selection(b, 1)))));
        register(a);
        assertThrows(DocumentException.class, () -> as(() -> selection.resolve(work, p.id(), p.revision(), List.of(new DocumentSelectionService.Selection(a, 1)))));
        assertThrows(DocumentException.class, () -> as(() -> selection.resolve(work, p.id(), p.revision() + 1, List.of(new DocumentSelectionService.Selection(a, 2)))));
        as(() -> templates.update(a, "Unavailable", "SAME_TYPE", TemplateState.UNAVAILABLE));
        assertEquals(1, as(() -> selection.options(work, 0, 50)).getFirst().templates().size());
        jdbc.update("update works set deleted_at = now() where id_work = ?", work);
        assertThrows(com.brica.works.application.WorkNotFoundException.class, () -> as(() -> selection.options(work, 0, 50)));
    }
    @Test void shouldPreserveBusinessRowsAcrossVersionAndPackageChanges() {
        long client = client(); long work = work(client);
        String before = business(client, work);
        long a = template("PRESERVE_A"); long b = template("PRESERVE_B"); register(a); register(b);
        assertEquals(before, business(client, work));
        var p = as(() -> packages.create(client, "Preservation", List.of(a), true));
        as(() -> packages.update(p.id(), DocumentPackageService.etag(p.revision()), client, "Edited", List.of(b), true));
        assertEquals(before, business(client, work));
    }
    String business(long client, long work) {
        return jdbc.queryForObject("select row_to_json(c)::text from cliente c where id_cliente = ?", String.class, client)
                + jdbc.queryForObject("select row_to_json(w)::text from works w where id_work = ?", String.class, work);
    }
    @Test void shouldProtectNewHttpEndpointsWithRealSessionsIndependentPermissionsAndCsrf() throws Exception {
        long id = template("HTTP"); register(id);
        for (String path : List.of("/document-templates", "/document-packages", "/document-templates/" + id + "/versions")) {
            assertEquals(401, new Session().get(path).statusCode());
        }
        for (var permissions : List.of(List.<String>of(), List.of("DOCUMENT_CONFIG_ADMIN"), List.of("DOCUMENT_VALUES_READ"), List.of("DOCUMENT_CONFIG_ADMIN", "DOCUMENT_VALUES_READ"))) {
            String username = "DocUser" + UUID.randomUUID().toString().replace("-", "");
            SecurityTestData.install(jdbc, passwords, username, permissions.toArray(String[]::new));
            var session = new Session(); session.login(username);
            boolean admin = permissions.contains("DOCUMENT_CONFIG_ADMIN");
            assertEquals(admin ? 200 : 403, session.get("/document-templates").statusCode());
            assertEquals(admin ? 200 : 403, session.get("/document-templates/" + id).statusCode());
            assertEquals(admin ? 200 : 403, session.get("/document-packages").statusCode());
            assertEquals(admin ? 200 : 403, session.get("/document-templates/" + id + "/versions/1/file").statusCode());
            assertEquals(admin ? 200 : 403, session.multipart("/document-templates/" + id + "/analyses", fixture("acta-inicio-prototipo"), null).statusCode());
            long work = work(client());
            assertEquals(permissions.contains("DOCUMENT_VALUES_READ") ? 200 : 403,
                    session.get("/works/" + work + "/document-options").statusCode());
            assertEquals(403, session.send("POST", "/document-templates", "{\"name\":\"No CSRF\",\"documentType\":\"X\",\"state\":\"CURRENT\"}", false).statusCode());
            assertEquals(admin ? 201 : 403, session.send("POST", "/document-templates", "{\"name\":\"HTTP\",\"documentType\":\"X\",\"state\":\"DRAFT\"}", true).statusCode());
        }
    }
    @Test void shouldRoundTripMultipartConfirmationAndReturnUpdatedReviewWithProblemDetails() throws Exception {
        long id = template("MULTIPART");
        var session = new Session(); session.login("IntegrationAdmin");
        var response = session.multipart("/document-templates/" + id + "/analyses", fixture("acta-inicio-prototipo"), null);
        assertEquals(200, response.statusCode(), response.body());
        var review = JSON.readValue(response.body(), TemplateAnalysisService.Review.class);
        var key = UUID.randomUUID();
        var metadata = Map.of("confirmationId", key.toString(), "fingerprint", review.fingerprint(), "required", required(review), "staticAccepted", false);
        var confirmation = session.multipart("/document-templates/" + id + "/versions", fixture("acta-inicio-prototipo"), metadata);
        assertEquals(200, confirmation.statusCode(), confirmation.body());
        assertEquals("COMPLETED", JSON.readTree(confirmation.body()).path("status").asText());
        assertEquals(confirmation.body(), session.multipart("/document-templates/" + id + "/versions", fixture("acta-inicio-prototipo"), metadata).body());
        assertEquals(200, session.get("/document-registrations/" + key).statusCode());
        var changed = session.multipart("/document-templates/" + id + "/versions", fixture("documento-estatico"),
                Map.of("confirmationId", UUID.randomUUID().toString(), "fingerprint", review.fingerprint(), "required", Map.of(), "staticAccepted", true));
        assertEquals(409, changed.statusCode(), changed.body());
        assertTrue(changed.headers().firstValue("Content-Type").orElse("").contains("application/problem+json"));
        assertEquals("TEMPLATE_REVIEW_CHANGED", JSON.readTree(changed.body()).path("code").asText());
        assertTrue(JSON.readTree(changed.body()).path("updatedAnalysis").path("staticDocument").asBoolean());
        Long field = review.fields().getFirst().id();
        assertEquals(400, session.send("PUT", "/reference-fields/" + field, "{\"visibleName\":\"Valid\",\"key\":\"CHANGED\"}", true).statusCode());
    }
    class Session {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();
        String token, header;
        URI uri(String path) { return URI.create("http://localhost:" + port + "/api/v1" + path); }
        HttpResponse<String> get(String path) throws Exception { return http.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        void csrf() throws Exception {
            var body = JSON.readTree(get("/auth/csrf").body()); token = body.path("token").asText(); header = body.path("headerName").asText();
        }
        void login(String name) throws Exception {
            csrf();
            var response = http.send(HttpRequest.newBuilder(uri("/auth/login")).header(header, token)
                    .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(
                            "username=" + name + "&password=TestUser123")).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode()); csrf();
        }
        HttpResponse<String> send(String method, String path, String body, boolean csrf) throws Exception {
            var builder = HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
            if (csrf) { builder.header(header, token); }
            return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> multipart(String path, byte[] bytes, Object metadata) throws Exception {
            String boundary = "test-" + UUID.randomUUID(); var body = new java.io.ByteArrayOutputStream();
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.docx\"\r\nContent-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(bytes); body.write("\r\n".getBytes(StandardCharsets.UTF_8));
            if (metadata != null) {
                body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"metadata\"\r\nContent-Type: application/json\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                body.write(JSON.writeValueAsBytes(metadata)); body.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return http.send(HttpRequest.newBuilder(uri(path)).header(header, token)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
