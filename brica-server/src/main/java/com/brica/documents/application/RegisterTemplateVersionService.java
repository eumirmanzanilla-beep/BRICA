package com.brica.documents.application;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import com.brica.documents.application.port.*;
import com.brica.documents.domain.*;
import com.brica.server.shared.validation.domain.InputRules;
import com.brica.server.shared.validation.domain.InvalidDataException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Two database transactions surround immutable file publication; PENDING is durable before I/O. */
@Service
@PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN')")
public class RegisterTemplateVersionService {
    private final DocumentConfigurationStore store;
    private final DocumentFileStore files;
    private final ReferenceFieldStore fields;
    private final TemplateAnalysisService analysis;
    private final TransactionTemplate tx;
    private final JsonMapper json;
    public RegisterTemplateVersionService(DocumentConfigurationStore store, DocumentFileStore files,
            ReferenceFieldStore fields, TemplateAnalysisService analysis, PlatformTransactionManager transactions, JsonMapper json) {
        this.store = store; this.files = files; this.fields = fields; this.analysis = analysis;
        this.tx = new TransactionTemplate(transactions); this.json = json;
    }
    public Confirmation confirm(Long templateId, UUID confirmationId, String name, byte[] content,
            String reviewedFingerprint, Map<Long, Boolean> required, boolean staticAccepted) {
        return confirm(templateId, confirmationId, name, content, reviewedFingerprint, required, staticAccepted, "Version_Nueva");
    }
    public Confirmation confirm(Long templateId, UUID confirmationId, String name, byte[] content,
            String reviewedFingerprint, Map<Long, Boolean> required, boolean staticAccepted, String versionName) {
        if (confirmationId == null || content == null || required == null || required.values().stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidDataException(Map.of("confirmation", "Indica confirmación y clasificación completas."));
        }
        var errors = new java.util.LinkedHashMap<String, String>();
        InputRules.requiredText(errors, "filename", name, 255);
        InputRules.validName(errors, "name", versionName, 150); InputRules.rejectErrors(errors);
        String hash = DocumentHashes.sha256(content);
        String requestHash = DocumentHashes.sha256(json.writeValueAsString(List.of(templateId, name, hash,
                reviewedFingerprint == null ? "" : reviewedFingerprint, new TreeMap<>(required), staticAccepted, versionName)));
        var started = tx.execute(status -> {
            var template = store.template(templateId, true);
            var previous = store.registration(confirmationId, true);
            if (previous.isPresent()) {
                if (!previous.get().getRequestHash().equals(requestHash)) {
                    throw new DocumentException(409, "CONFIRMATION_CONFLICT", "Esa confirmación ya identifica otra solicitud.");
                }
                return new Started(result(previous.get()), null, false);
            }
            template.requireEditable();
            var reviewed = analysis.analyze(templateId, content);
            if (!reviewed.fingerprint().equals(reviewedFingerprint)) {
                return new Started(null, reviewed, false);
            }
            if (!reviewed.valid()) { throw new DocumentException(400, "INVALID_TEMPLATE", "Corrige los marcadores señalados antes de registrar la versión."); }
            var expected = reviewed.fields().stream().map(TemplateAnalysisService.DetectedField::id).collect(Collectors.toSet());
            if (!expected.equals(required.keySet()) || reviewed.staticDocument() && !staticAccepted) {
                throw new InvalidDataException(Map.of("required", "Clasifica todos los campos o confirma expresamente el documento estático."));
            }
            var op = new RegistrationOperation(confirmationId, templateId, requestHash, hash, name, content.length,
                    json.writeValueAsString(reviewed), json.writeValueAsString(new TreeMap<>(required)));
            op.nameVersion(versionName);
            store.add(op); store.flush();
            return new Started(result(op), null, true);
        });
        if (started.updatedAnalysis() != null) { return new Confirmation(null, started.updatedAnalysis()); }
        if (!started.created()) { return new Confirmation(started.operation(), null); }
        try { files.put(confirmationId, content); }
        catch (DocumentException exception) {
            markFailed(confirmationId);
            return new Confirmation(status(confirmationId), null);
        }
        return new Confirmation(recover(confirmationId), null);
    }
    public OperationView status(UUID id) {
        return tx.execute(s -> result(store.registration(id, false).orElseThrow(() -> missing())));
    }
    public OperationView recover(UUID id) {
        // Always lock template before operation to keep lock ordering consistent across retries.
        try {
            return tx.execute(s -> {
            var observed = store.registration(id, false).orElseThrow(() -> missing());
            var template = store.template(observed.getTemplateId(), true);
            var op = store.registration(id, true).orElseThrow(() -> missing());
            if (op.getStatus() != RegistrationOperation.Status.PENDING) { return result(op); }
            byte[] content;
            try { content = files.read(files.relativePath(id)); }
            catch (DocumentException exception) { op.fail(); return result(op); }
            if (template.getDeletedAt() != null || content.length != op.getSizeBytes()
                    || !DocumentHashes.sha256(content).equals(op.getFileHash())) { op.fail(); return result(op); }
            var required = json.readValue(op.getRequirementsJson(), new TypeReference<Map<Long, Boolean>>() { });
            var linked = fields.findAllById(required.keySet());
            if (linked.size() != required.size()) { op.fail(); return result(op); }
            int number = store.versions(template.getId()).stream().mapToInt(v -> v.getId().number()).max().orElse(0) + 1;
            var review = json.readValue(op.getAnalysisJson(), TemplateAnalysisService.Review.class);
            var file = new DocumentFile(op.getOriginalName(), files.relativePath(id), op.getSizeBytes(), op.getFileHash());
            store.add(file); store.flush();
            var version = new TemplateVersion(template.getId(), number, file.getId(), review.staticDocument(), op.getAnalysisJson());
            version.rename(op.getVersionName());
            store.add(version);
            for (var field : linked) { store.add(new TemplateVersionField(version.getId(), field, required.get(field.getId()))); }
            store.flush();
            op.complete(number);
            return result(op);
            });
        } catch (org.springframework.dao.DataAccessException exception) {
            throw new DocumentException(503, "REGISTRATION_PENDING", "No se completó el registro. Conserva su ID y consulta o recupera la operación antes de crear otra confirmación.");
        }
    }
    private void markFailed(UUID id) {
        tx.executeWithoutResult(s -> store.registration(id, true).ifPresent(op -> {
            if (op.getStatus() == RegistrationOperation.Status.PENDING) { op.fail(); }
        }));
    }
    private OperationView result(RegistrationOperation op) {
        return new OperationView(op.getId(), op.getTemplateId(), op.getStatus(), op.getVersionNumber(), op.getFailure());
    }
    private DocumentException missing() { return new DocumentException(404, "REGISTRATION_NOT_FOUND", "No existe la confirmación solicitada."); }
    private record Started(OperationView operation, TemplateAnalysisService.Review updatedAnalysis, boolean created) { }
    public record OperationView(UUID id, Long templateId, RegistrationOperation.Status status, Integer versionNumber, String failure) { }
    public record Confirmation(OperationView operation, TemplateAnalysisService.Review updatedAnalysis) { }
}
