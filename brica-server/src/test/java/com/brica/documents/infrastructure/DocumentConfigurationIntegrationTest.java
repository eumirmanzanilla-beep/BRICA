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
    String uniqueName(String prefix) { return prefix + UUID.randomUUID().toString().replace("-", ""); }
    long template(String type) { return as(() -> templates.create(uniqueName("Formato_"), type, TemplateState.CURRENT).id()); }
    TemplateAnalysisService.Review review(long id) { return as(() -> analyses.analyze(id, fixture("acta-inicio-prototipo"))); }
    Map<Long, Boolean> required(TemplateAnalysisService.Review review) {
        var result = new TreeMap<Long, Boolean>(); review.fields().forEach(f -> result.put(f.id(), true)); return result;
    }
    RegisterTemplateVersionService.OperationView register(long id) {
        var r = review(id);
        return as(() -> versions.confirm(id, UUID.randomUUID(), "test.docx", fixture("acta-inicio-prototipo"), r.fingerprint(), required(r), false).operation());
    }
    long client() { return jdbc.queryForObject("insert into cliente(nombre, representante_legal) values ('Cliente_prueba', 'Representante') returning id_cliente", Long.class); }
    long work(long client) { return jdbc.queryForObject("insert into works(client_id, name, location, start_date) values (?, 'Obra_prueba', 'Mérida', '2026-01-01') returning id_work", Long.class, client); }
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
    @Test void shouldKeepWorkAndClientDocumentScopesSeparate() {
        long clientId = client(), a = work(clientId), b = work(clientId), otherClient = client();
        long scoped = as(() -> templates.create(uniqueName("Obra_"), "ACTA", TemplateState.CURRENT, null, a)).id();
        register(scoped);
        var workPackage = as(() -> packages.createFromWork(a, "Paquete_exclusivo", List.of(scoped), true));
        assertEquals(a, workPackage.workId());
        assertTrue(as(() -> packages.listForClient(clientId, 0, 100)).stream().anyMatch(p -> p.id().equals(workPackage.id())));
        assertTrue(as(() -> packages.listForWork(b, 0, 100)).stream().noneMatch(p -> p.id().equals(workPackage.id())));
        assertThrows(DocumentException.class, () -> as(() -> packages.createFromWork(b, "Ajeno", List.of(scoped), true)));
        assertThrows(DocumentException.class, () -> as(() -> packages.create(clientId, "No_ampliar_ambito", List.of(scoped), true)));
        assertTrue(as(() -> selection.options(b, 0, 100)).stream().noneMatch(p -> p.id().equals(workPackage.id())));
        assertThrows(DocumentException.class, () -> as(() -> selection.resolve(b, workPackage.id(), workPackage.revision(),
                List.of(new DocumentSelectionService.Selection(scoped, 1)))));
        long shared = as(() -> templates.create(uniqueName("Cliente_"), "COMPARTIDA", TemplateState.CURRENT, clientId, null)).id();
        register(shared);
        var clientPackage = as(() -> packages.create(clientId, "Paquete_del_cliente", List.of(shared), true));
        assertTrue(as(() -> selection.options(a, 0, 100)).stream().anyMatch(p -> p.id().equals(clientPackage.id())));
        assertTrue(as(() -> selection.options(b, 0, 100)).stream().anyMatch(p -> p.id().equals(clientPackage.id())));
        assertThrows(DocumentException.class, () -> as(() -> packages.create(otherClient, "No_otro_cliente", List.of(shared), true)));
    }
    @Test void shouldCreateTemplateInsidePackageAndRequireItsVersionBeforeConfirmation() {
        long clientId = client(), workId = work(clientId);
        var draft = as(() -> packages.createFromWork(workId, "Primero_nombre", List.of(), false));
        var result = as(() -> packages.createTemplate(draft.id(), DocumentPackageService.etag(draft.revision()), uniqueName("Dentro_"), "ACTA"));
        assertEquals(clientId, result.template().clientId()); assertEquals(workId, result.template().workId());
        assertEquals(List.of(result.template().id()), result.documentPackage().templates().stream().map(DocumentPackageService.PackageTemplate::id).toList());
        assertThrows(DocumentException.class, () -> as(() -> packages.updateFromWork(draft.id(), DocumentPackageService.etag(result.documentPackage().revision()),
                workId, draft.name(), List.of(result.template().id()), true)));
        register(result.template().id());
        var saved = as(() -> packages.updateFromWork(draft.id(), DocumentPackageService.etag(result.documentPackage().revision()),
                workId, draft.name(), List.of(result.template().id()), true));
        assertEquals(DocumentPackage.State.CONFIRMED, saved.state());
    }
    @Test void shouldCreateMinimalWorkAndAtomicallyEditItsCompleteInformation() throws Exception {
        var admin = new Session(); admin.login("IntegrationAdmin");
        var created = admin.send("POST", "/works", "{\"clientId\":" + client() + ",\"name\":\"Obra_minima\",\"description\":\"Descripción\"}", true);
        assertEquals(201, created.statusCode(), created.body());
        long id = JSON.readTree(created.body()).path("id").asLong();
        var initial = admin.get("/works/" + id + "/information");
        String etag = initial.headers().firstValue("ETag").orElseThrow();
        var info = Map.of("name", "Nombre_editado", "location", "Ubicación opcional", "description", "Nueva descripción", "documentInformation",
                Map.of("status", "ON_HOLD", "additionalValues", Map.of(), "contractedAmount", "1.186"));
        var saved = admin.sendWithEtag("PUT", "/works/" + id + "/information", JSON.writeValueAsString(info), etag);
        assertEquals(200, saved.statusCode(), saved.body());
        assertEquals("Nombre_editado", JSON.readTree(saved.body()).path("work").path("name").asText());
        assertEquals("1.186", jdbc.queryForObject("SELECT contracted_amount::text FROM works WHERE id_work=?", String.class, id));
        assertEquals(412, admin.sendWithEtag("PUT", "/works/" + id + "/information", JSON.writeValueAsString(info), etag).statusCode());
        var invalid = new LinkedHashMap<String, Object>(info); invalid.put("name", " ");
        assertEquals(400, admin.sendWithEtag("PUT", "/works/" + id + "/information", JSON.writeValueAsString(invalid), saved.headers().firstValue("ETag").orElseThrow()).statusCode());
        assertEquals("Nombre_editado", jdbc.queryForObject("SELECT name FROM works WHERE id_work=?", String.class, id));
    }
    @Test void shouldAppendSeveralTemplatesBeforeAndAfterConfirmationWithoutReplacingExistingOnes() {
        long clientId = client();
        var draft = as(() -> packages.create(clientId, "Multiple", List.of(), false));
        var first = as(() -> packages.createTemplate(draft.id(), DocumentPackageService.etag(draft.revision()), uniqueName("Primera_"), "ACTA"));
        var second = as(() -> packages.createTemplate(draft.id(), DocumentPackageService.etag(first.documentPackage().revision()), uniqueName("Segunda_"), "ACTA"));
        assertEquals(2, second.documentPackage().templates().size());
        register(first.template().id()); register(second.template().id());
        var confirmed = as(() -> packages.update(draft.id(), DocumentPackageService.etag(second.documentPackage().revision()), clientId,
                draft.name(), List.of(first.template().id(), second.template().id()), true));
        var third = as(() -> packages.createTemplate(draft.id(), DocumentPackageService.etag(confirmed.revision()), uniqueName("Tercera_"), "OTRA"));
        assertEquals(DocumentPackage.State.CONFIRMED, third.documentPackage().state());
        assertEquals(2, third.documentPackage().templates().size(), "La composición confirmada sigue válida durante la preparación");
        assertThrows(DocumentException.class, () -> as(() -> packages.update(draft.id(), DocumentPackageService.etag(confirmed.revision()), clientId,
                draft.name(), List.of(first.template().id(), second.template().id(), third.template().id()), false)));
        register(third.template().id());
        var appended = as(() -> packages.update(draft.id(), DocumentPackageService.etag(confirmed.revision()), clientId,
                draft.name(), List.of(first.template().id(), second.template().id(), third.template().id()), false));
        assertEquals(3, appended.templates().size()); assertEquals(DocumentPackage.State.CONFIRMED, appended.state());
        assertThrows(DocumentException.class, () -> as(() -> packages.createTemplate(draft.id(), DocumentPackageService.etag(confirmed.revision()), uniqueName("Obsoleta_"), "X")));
        long a = work(clientId), b = work(clientId);
        assertTrue(as(() -> packages.listForWork(a, 0, 100)).stream().anyMatch(p -> p.id().equals(draft.id())));
        assertTrue(as(() -> packages.listForWork(b, 0, 100)).stream().anyMatch(p -> p.id().equals(draft.id())));
    }
    @Test void shouldRenameAndLogicallyDeleteVersionsPreservingFilesAndNeverReusingNumbers() throws Exception {
        long id = template("VERSION_LIFECYCLE"); register(id); register(id);
        var admin = new Session(); admin.login("IntegrationAdmin");
        var before = as(() -> templates.version(id, 2)); byte[] bytes = as(() -> templates.download(id, 2));
        var renamed = admin.send("PUT", "/document-templates/" + id + "/versions/2/name", "{\"name\":\"Revision_Ñ2\"}", true);
        assertEquals(200, renamed.statusCode(), renamed.body());
        assertEquals("Revision_Ñ2", as(() -> templates.version(id, 2)).name());
        assertEquals(before.sha256(), as(() -> templates.version(id, 2)).sha256());
        assertArrayEquals(bytes, as(() -> templates.download(id, 2)));
        for (String invalid : List.of("123", "_Version", "Dos palabras", "Versión")) {
            assertEquals(400, admin.send("PUT", "/document-templates/" + id + "/versions/2/name", JSON.writeValueAsString(Map.of("name", invalid)), true).statusCode());
        }
        long clientId = client(), workId = work(clientId);
        var p = as(() -> packages.create(clientId, "Versiones", List.of(id), true));
        assertEquals(204, admin.send("DELETE", "/document-templates/" + id + "/versions/2", "", true).statusCode());
        assertTrue(as(() -> templates.version(id, 2)).deleted());
        assertEquals(2, as(() -> templates.versions(id)).size());
        assertArrayEquals(bytes, as(() -> templates.download(id, 2)), "Consulta histórica conserva el archivo");
        assertEquals(1, as(() -> templates.get(id)).latestVersion());
        assertEquals(1, as(() -> selection.options(workId, 0, 100)).getFirst().templates().getFirst().version());
        assertThrows(DocumentException.class, () -> as(() -> selection.resolve(workId, p.id(), p.revision(), List.of(new DocumentSelectionService.Selection(id, 2)))));
        Long linkedField = before.fields().getFirst().id();
        assertThrows(DocumentRuleException.class, () -> as(() -> { updateFields.delete(linkedField); return null; }));
        assertEquals(3, register(id).versionNumber(), "No se reutiliza el número de una versión eliminada");
        String readerName = uniqueName("VersionReader"); SecurityTestData.install(jdbc, passwords, readerName, "DOCUMENT_VALUES_READ");
        var reader = new Session(); reader.login(readerName);
        assertEquals(403, reader.send("DELETE", "/document-templates/" + id + "/versions/1", "", true).statusCode());
        assertEquals(403, reader.send("PUT", "/document-templates/" + id + "/versions/1/name", "{\"name\":\"Nueva\"}", true).statusCode());
        as(() -> { templates.deleteVersion(id, 1); templates.deleteVersion(id, 3); return null; });
        assertNull(as(() -> templates.get(id)).latestVersion());
        assertTrue(as(() -> selection.options(workId, 0, 100)).isEmpty());
        assertThrows(DocumentRuleException.class, () -> as(() -> templates.renameVersion(id, 2, "No_renombrar_eliminada")));
        assertThrows(DocumentRuleException.class, () -> as(() -> { updateFields.delete(linkedField); return null; }));
        assertEquals(4, register(id).versionNumber());
    }
    @Test void shouldKeepTechnicalNamesForDocumentsAcceptReadableWorkNamesAndRejectReversedDatesThroughHttp() throws Exception {
        var admin = new Session(); admin.login("IntegrationAdmin"); long clientId = client();
        for (String name : List.of("123", "123_456", "_Nombre", "Con espacio", "Conácento", "A.B")) {
            assertEquals(400, admin.send("POST", "/clients", JSON.writeValueAsString(Map.of("name", name, "legalRepresentative", "Representante")), true).statusCode());
            assertEquals(201, admin.send("POST", "/works", JSON.writeValueAsString(Map.of("name", name, "clientId", clientId)), true).statusCode());
            assertEquals(400, admin.send("POST", "/document-templates", JSON.writeValueAsString(Map.of("name", name, "documentType", "X", "state", "CURRENT")), true).statusCode());
            assertEquals(400, admin.send("POST", "/document-packages", JSON.writeValueAsString(Map.of("name", name, "clientId", clientId, "templateIds", List.of(), "confirm", false)), true).statusCode());
        }
        long id = work(clientId); var initial = admin.get("/works/" + id + "/information"); String etag = initial.headers().firstValue("ETag").orElseThrow();
        for (String key : List.of("endDate", "actualEndDate")) {
            var body = Map.of("name", "Obra_Ñ", "documentInformation", Map.of("startDate", "2026-02-01", key, "2026-01-31", "status", "IN_PROGRESS", "additionalValues", Map.of()));
            assertEquals(400, admin.sendWithEtag("PUT", "/works/" + id + "/information", JSON.writeValueAsString(body), etag).statusCode());
            assertEquals(etag, admin.get("/works/" + id + "/information").headers().firstValue("ETag").orElseThrow());
        }
    }
    @Test void shouldPersistAndDownloadAttachmentsOnlyForTheirWork() throws Exception {
        long workId = work(client()), other = work(client()); var admin = new Session(); admin.login("IntegrationAdmin");
        byte[] pdf = "%PDF-1.7\nDocumento construido de prueba\n%%EOF".getBytes(StandardCharsets.UTF_8);
        var upload = admin.attachment(workId, "CONTRACT", "contrato.pdf", pdf);
        assertEquals(201, upload.statusCode(), upload.body());
        long id = JSON.readTree(upload.body()).path("id").asLong();
        var download = admin.http.send(HttpRequest.newBuilder(admin.uri("/works/" + workId + "/attachments/" + id + "/file")).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, download.statusCode()); assertArrayEquals(pdf, download.body());
        assertEquals(404, admin.get("/works/" + other + "/attachments/" + id + "/file").statusCode());
        assertEquals(400, admin.attachment(workId, "CONTRACT", "fake.pdf", new byte[]{1, 2, 3}).statusCode());
        assertEquals(201, admin.attachment(workId, "BASE_BUDGET", "base.docx", fixture("documento-estatico")).statusCode());
        byte[] larger = new byte[1048576 + 100]; System.arraycopy(pdf, 0, larger, 0, pdf.length);
        var largeUpload = admin.attachment(workId, "BASE_BUDGET", "presupuesto-grande.pdf", larger);
        assertEquals(201, largeUpload.statusCode(), largeUpload.body());
        long largeId = JSON.readTree(largeUpload.body()).path("id").asLong();
        var largeDownload = admin.http.send(HttpRequest.newBuilder(admin.uri("/works/" + workId + "/attachments/" + largeId + "/file")).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, largeDownload.statusCode()); assertArrayEquals(larger, largeDownload.body());
        String reader = "AttachmentReader" + UUID.randomUUID().toString().replace("-", "");
        SecurityTestData.install(jdbc, passwords, reader, "WORKS_READ"); var session = new Session(); session.login(reader);
        assertEquals(200, session.get("/works/" + workId + "/attachments").statusCode());
        assertEquals(403, session.attachment(workId, "CONTRACT", "otro.pdf", pdf).statusCode());
        assertEquals(403, session.send("DELETE", "/works/" + workId + "/attachments/" + id, "", true).statusCode());
        assertEquals(404, admin.send("DELETE", "/works/" + other + "/attachments/" + id, "", true).statusCode());
        assertEquals(204, admin.send("DELETE", "/works/" + workId + "/attachments/" + id, "", true).statusCode());
        assertEquals(404, admin.get("/works/" + workId + "/attachments/" + id + "/file").statusCode());
        String retainedPath = jdbc.queryForObject("SELECT relative_path FROM work_attachments WHERE id_attachment=? AND deleted_at IS NOT NULL", String.class, id);
        assertArrayEquals(pdf, Files.readAllBytes(STORAGE.resolve(retainedPath)));
        var visibleAttachments = JSON.readTree(admin.get("/works/" + workId + "/attachments").body());
        for (var attachment : visibleAttachments) { assertNotEquals(id, attachment.path("id").asLong()); }
    }
    @Test void shouldDeleteAnUnlinkedFieldAndProtectLinkedFieldOverHttp() throws Exception {
        var free = as(() -> registerFields.register("FREE_" + UUID.randomUUID().toString().replace("-", "").toUpperCase(),
                "Campo sin vínculos", ReferenceAttribute.WORK_NAME));
        var admin = new Session(); admin.login("IntegrationAdmin");
        assertEquals(204, admin.send("DELETE", "/reference-fields/" + free.getId(), "", true).statusCode());
        assertEquals(0L, jdbc.queryForObject("select count(*) from reference_fields where id_reference_field = ?", Long.class, free.getId()));
        long template = template("DELETE_FIELD"); var r = review(template); register(template);
        Long linkedId = r.fields().getFirst().id();
        var response = admin.send("DELETE", "/reference-fields/" + linkedId, "", true);
        assertEquals(409, response.statusCode(), response.body());
        assertEquals("REFERENCE_FIELD_IN_USE", JSON.readTree(response.body()).path("code").asText());
        assertEquals(1L, jdbc.queryForObject("select count(*) from reference_fields where id_reference_field = ?", Long.class, linkedId));
        as(() -> { templates.delete(template); return null; });
        var historical = admin.send("DELETE", "/reference-fields/" + linkedId, "", true);
        assertEquals(409, historical.statusCode(), historical.body());
        assertTrue(JSON.readTree(historical.body()).path("detail").asText().contains("históricos"));
        String username = "ReaderDelete" + UUID.randomUUID().toString().replace("-", "");
        SecurityTestData.install(jdbc, passwords, username, "DOCUMENT_VALUES_READ");
        var reader = new Session(); reader.login(username);
        assertEquals(403, reader.send("DELETE", "/reference-fields/" + linkedId, "", true).statusCode());
    }
    @Test void shouldDeleteEachUnlinkedApprovedAttributeAndAdditionalTypeOverHttp() throws Exception {
        var admin = new Session(); admin.login("IntegrationAdmin");
        String prefix = "DEL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase() + "_";
        for (var attribute : ReferenceAttribute.values()) {
            var created = admin.send("POST", "/reference-fields", JSON.writeValueAsString(Map.of(
                    "key", prefix + attribute.name(), "visibleName", "Campo borrable", "attribute", attribute.name())), true);
            assertEquals(201, created.statusCode(), created.body());
            long id = JSON.readTree(created.body()).path("id").asLong();
            var deleted = admin.send("DELETE", "/reference-fields/" + id, "", true);
            assertEquals(204, deleted.statusCode(), attribute + ": " + deleted.body());
            assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM reference_fields WHERE id_reference_field=?", Long.class, id));
        }
        for (var type : ReferenceDataType.values()) {
            var created = admin.send("POST", "/reference-fields", JSON.writeValueAsString(Map.of(
                    "key", prefix + type.name(), "visibleName", "Adicional borrable", "additionalType", type.name())), true);
            assertEquals(201, created.statusCode(), created.body());
            long id = JSON.readTree(created.body()).path("id").asLong();
            var deleted = admin.send("DELETE", "/reference-fields/" + id, "", true);
            assertEquals(204, deleted.statusCode(), type + ": " + deleted.body());
            assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM reference_fields WHERE id_reference_field=?", Long.class, id));
        }
    }
    @Test void shouldPersistPreciseAndTypedWorkValuesWithoutChangingOtherWorks() throws Exception {
        long client = client(), first = work(client), second = work(client);
        var admin = new Session(); admin.login("IntegrationAdmin");
        String key = "IMPORTE_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        var created = admin.send("POST", "/reference-fields", "{\"key\":\"" + key
                + "\",\"visibleName\":\"Importe adicional\",\"additionalType\":\"AMOUNT\"}", true);
        assertEquals(201, created.statusCode(), created.body());
        long id = JSON.readTree(created.body()).path("id").asLong();
        var current = admin.get("/works/" + first + "/document-information");
        assertEquals(200, current.statusCode(), current.body());
        String etag = current.headers().firstValue("ETag").orElseThrow();
        String body = "{\"contractNumber\":\"001\",\"contractor\":\"Prueba\",\"contractedAmount\":1.186123456789,"
                + "\"amountBeforeTax\":1.184,\"taxAmount\":0,\"startDate\":\"2005-07-16\",\"endDate\":null,"
                + "\"actualEndDate\":null,\"status\":\"ON_HOLD\",\"additionalValues\":{\"" + key + "\":\"1.186\"}}";
        var saved = admin.sendWithEtag("PUT", "/works/" + first + "/document-information", body, etag);
        assertEquals(200, saved.statusCode(), saved.body());
        assertEquals("1.186123456789", jdbc.queryForObject("select contracted_amount::text from works where id_work = ?", String.class, first));
        assertEquals("IN_PROGRESS", jdbc.queryForObject("select status from works where id_work = ?", String.class, second));
        assertEquals(412, admin.sendWithEtag("PUT", "/works/" + first + "/document-information", body, etag).statusCode());
        var value = admin.send("POST", "/reference-values/resolve", "{\"workId\":" + first + ",\"fieldIds\":[" + id + "]}", true);
        assertEquals(200, value.statusCode(), value.body());
        assertEquals("1.186", JSON.readTree(value.body()).path(0).path("textValue").asText());
        var absent = admin.send("POST", "/reference-values/resolve", "{\"workId\":" + second + ",\"fieldIds\":[" + id + "]}", true);
        assertEquals("MISSING", JSON.readTree(absent.body()).path(0).path("status").asText());
        assertEquals(204, admin.send("DELETE", "/reference-fields/" + id, "", true).statusCode());
        String latestEtag = saved.headers().firstValue("ETag").orElseThrow();
        var unchanged = admin.sendWithEtag("PUT", "/works/" + first + "/document-information", body, latestEtag);
        assertEquals(200, unchanged.statusCode(), unchanged.body());
        String omitted = body.replace("\"" + key + "\":\"1.186\"", "");
        var retained = admin.sendWithEtag("PUT", "/works/" + first + "/document-information", omitted,
                unchanged.headers().firstValue("ETag").orElseThrow());
        assertEquals(200, retained.statusCode(), retained.body());
        assertEquals("1.186", JSON.readTree(retained.body()).path("additionalValues").path(key).asText());
        var altered = admin.sendWithEtag("PUT", "/works/" + first + "/document-information", body.replace("1.186\"", "2.000\""),
                retained.headers().firstValue("ETag").orElseThrow());
        assertEquals(400, altered.statusCode(), altered.body());
    }
    @Test void shouldEnforceExactTemplateNamesInPostgres() {
        String name = uniqueName("Formato_");
        as(() -> templates.create(name, "ACTA", TemplateState.DRAFT));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> as(() -> templates.create(name, "OTHER", TemplateState.DRAFT)));
        assertNotNull(as(() -> templates.create(name.toLowerCase(java.util.Locale.ROOT), "ACTA", TemplateState.DRAFT)));
        assertNotNull(as(() -> templates.create(name.replace("_", ""), "ACTA", TemplateState.DRAFT)));
    }
    @Test void shouldDerivePackageClientFromWorkAndRequireWorkRead() throws Exception {
        long clientId = client(), workId = work(clientId);
        var admin = new Session(); admin.login("IntegrationAdmin");
        String request = "{\"workId\":" + workId + ",\"name\":\"Desde_obra\",\"templateIds\":[],\"confirm\":false}";
        var created = admin.send("POST", "/document-packages", request, true);
        assertEquals(201, created.statusCode(), created.body());
        var body = JSON.readTree(created.body());
        assertEquals(clientId, body.path("clientId").asLong());
        long packageId = body.path("id").asLong();
        var updated = admin.sendWithEtag("PUT", "/document-packages/" + packageId,
                request.replace("Desde_obra", "Desde_obra_editado"), created.headers().firstValue("ETag").orElseThrow());
        assertEquals(200, updated.statusCode(), updated.body());
        assertEquals(412, admin.sendWithEtag("PUT", "/document-packages/" + packageId,
                request, created.headers().firstValue("ETag").orElseThrow()).statusCode());
        assertEquals(400, admin.send("POST", "/document-packages", request.replace("\"workId\":", "\"clientId\":" + clientId + ",\"workId\":"), true).statusCode());
        String username = "DocOnly" + UUID.randomUUID().toString().replace("-", "");
        SecurityTestData.install(jdbc, passwords, username, "DOCUMENT_CONFIG_ADMIN");
        var docOnly = new Session(); docOnly.login(username);
        assertEquals(403, docOnly.send("POST", "/document-packages", request, true).statusCode());
        assertEquals(403, docOnly.get("/works").statusCode());
        assertEquals(403, docOnly.get("/clients").statusCode());
        jdbc.update("UPDATE works SET deleted_at=now() WHERE id_work=?", workId);
        assertEquals(404, admin.send("POST", "/document-packages", request, true).statusCode());
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
        var inUse = assertThrows(DocumentRuleException.class, () -> as(() -> { updateFields.delete(field); return null; }));
        assertEquals("REFERENCE_FIELD_IN_USE", inUse.code());
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
        assertTrue(as(() -> templates.list(0, 100)).stream().noneMatch(t -> t.id().equals(id)));
        assertTrue(as(() -> templates.list(0, 100, true)).stream().anyMatch(t -> t.id().equals(id) && t.deleted()));
        assertArrayEquals(fixture("acta-inicio-prototipo"), as(() -> templates.download(id, 1)));
        assertThrows(DocumentRuleException.class, () -> as(() -> templates.update(id, "Edit", "STATUS", TemplateState.CURRENT)));
        assertThrows(DocumentRuleException.class, () -> review(id));
    }
    @Test void shouldPersistDraftsUseExactNamesRejectStaleEditsAndKeepClientFixed() {
        long id = template("PACKAGE"); register(id); long client = client();
        var draft = as(() -> packages.create(null, "Borrador", List.of(id), false));
        var reopened = as(() -> packages.get(draft.id())); assertEquals(draft, reopened);
        var confirmed = as(() -> packages.update(draft.id(), DocumentPackageService.etag(draft.revision()), client, "Paquete", List.of(id), true));
        assertEquals(DocumentPackage.State.CONFIRMED, confirmed.state());
        assertThrows(DocumentException.class, () -> as(() -> packages.update(draft.id(), DocumentPackageService.etag(draft.revision()), client, "Lost", List.of(id), true)));
        assertThrows(DocumentRuleException.class, () -> as(() -> packages.update(draft.id(), DocumentPackageService.etag(confirmed.revision()), client(), "Change", List.of(id), true)));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> as(() -> packages.create(client, "Paquete", List.of(id), true)));
        assertNotNull(as(() -> packages.create(client, "paquete", List.of(id), true)));
        assertThrows(com.brica.server.shared.validation.domain.InvalidDataException.class,
                () -> as(() -> packages.create(client, " Paquete ", List.of(id), true)));
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
            assertEquals(admin ? 201 : 403, session.send("POST", "/document-templates", "{\"name\":\"HTTP_" + username + "\",\"documentType\":\"X\",\"state\":\"DRAFT\"}", true).statusCode());
        }
    }
    @Test void shouldRoundTripMultipartConfirmationAndReturnUpdatedReviewWithProblemDetails() throws Exception {
        long id = template("MULTIPART");
        var session = new Session(); session.login("IntegrationAdmin");
        var response = session.multipart("/document-templates/" + id + "/analyses", fixture("acta-inicio-prototipo"), null);
        assertEquals(200, response.statusCode(), response.body());
        var review = JSON.readValue(response.body(), TemplateAnalysisService.Review.class);
        var key = UUID.randomUUID();
        var metadata = Map.of("confirmationId", key.toString(), "fingerprint", review.fingerprint(), "required", required(review), "staticAccepted", false, "name", "Version_inicial");
        var confirmation = session.multipart("/document-templates/" + id + "/versions", fixture("acta-inicio-prototipo"), metadata);
        assertEquals(200, confirmation.statusCode(), confirmation.body());
        assertEquals("COMPLETED", JSON.readTree(confirmation.body()).path("status").asText());
        assertEquals(confirmation.body(), session.multipart("/document-templates/" + id + "/versions", fixture("acta-inicio-prototipo"), metadata).body());
        assertEquals(200, session.get("/document-registrations/" + key).statusCode());
        var changed = session.multipart("/document-templates/" + id + "/versions", fixture("documento-estatico"),
                Map.of("confirmationId", UUID.randomUUID().toString(), "fingerprint", review.fingerprint(), "required", Map.of(), "staticAccepted", true, "name", "Version_estatica"));
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
        HttpResponse<String> sendWithEtag(String method, String path, String body, String etag) throws Exception {
            return http.send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                    .header(header, token).header("If-Match", etag)
                    .method(method, HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
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
        HttpResponse<String> attachment(long workId, String kind, String filename, byte[] content) throws Exception {
            String boundary = "test-" + UUID.randomUUID(); var body = new java.io.ByteArrayOutputStream();
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"kind\"\r\n\r\n" + kind + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(content); body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return http.send(HttpRequest.newBuilder(uri("/works/" + workId + "/attachments")).header(header, token)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
