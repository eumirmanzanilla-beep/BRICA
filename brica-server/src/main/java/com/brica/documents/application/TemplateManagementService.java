package com.brica.documents.application;

import java.util.List;
import com.brica.documents.application.port.*;
import com.brica.documents.domain.*;
import com.brica.server.shared.validation.domain.InputRules;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN')")
@Transactional
public class TemplateManagementService {
    private final DocumentConfigurationStore store;
    private final DocumentFileStore files;
    private final ReferenceFieldStore fields;
    public TemplateManagementService(DocumentConfigurationStore store, DocumentFileStore files, ReferenceFieldStore fields) {
        this.store = store; this.files = files; this.fields = fields;
    }
    public TemplateView create(String name, String type, TemplateState state) {
        var template = new DocumentTemplate(name, type, state);
        store.add(template); store.flush();
        return view(template);
    }
    public TemplateView update(Long id, String name, String type, TemplateState state) {
        var template = store.template(id, true);
        template.change(name, type, state);
        store.flush();
        return view(template);
    }
    public void delete(Long id) { store.template(id, true).delete(); }
    @Transactional(readOnly = true)
    public TemplateView get(Long id) { InputRules.positiveId("templateId", id); return view(store.template(id, false)); }
    @Transactional(readOnly = true)
    public List<TemplateView> list(int page, int size) {
        pagination(page, size);
        var templates = store.templates(page, size);
        var latest = store.latestNumbers(templates.stream().map(DocumentTemplate::getId).toList());
        return templates.stream().map(t -> new TemplateView(t.getId(), t.getName(), t.getDocumentType(), t.getState(),
                t.getDeletedAt() != null, latest.get(t.getId()))).toList();
    }
    @Transactional(readOnly = true)
    public List<VersionView> versions(Long templateId) {
        store.template(templateId, false);
        return store.versions(templateId).stream().map(this::versionView).toList();
    }
    @Transactional(readOnly = true)
    public VersionView version(Long templateId, int number) { return versionView(store.version(new TemplateVersionId(templateId, number))); }
    @Transactional(readOnly = true)
    public byte[] download(Long templateId, int number) {
        var file = store.file(store.version(new TemplateVersionId(templateId, number)).getFileId());
        var content = files.read(file.getRelativePath());
        if (!DocumentHashes.sha256(content).equals(file.getSha256()) || content.length != file.getSizeBytes()) {
            throw new DocumentException(503, "DOCUMENT_INTEGRITY_ERROR", "El archivo no coincide con sus metadatos.");
        }
        return content;
    }
    private TemplateView view(DocumentTemplate t) {
        return new TemplateView(t.getId(), t.getName(), t.getDocumentType(), t.getState(), t.getDeletedAt() != null,
                store.latest(t.getId()).map(v -> v.getId().number()).orElse(null));
    }
    private VersionView versionView(TemplateVersion v) {
        var bindings = store.versionFields(v.getId());
        var byId = fields.findAllById(bindings.stream().map(TemplateVersionField::getFieldId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(ReferenceField::getId, java.util.function.Function.identity()));
        var details = bindings.stream().map(b -> new VersionField(b.getFieldId(), byId.get(b.getFieldId()).getKey(),
                byId.get(b.getFieldId()).getVisibleName(), b.getMarker(), b.isRequired())).toList();
        var file = store.file(v.getFileId());
        return new VersionView(v.getId().templateId(), v.getId().number(), "COMPLETED", v.isStaticDocument(),
                file.getOriginalName(), file.getSha256(), details, v.getAnalysisJson());
    }
    public static void pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new DocumentException(400, "INVALID_PAGINATION", "Usa una página positiva y entre 1 y 100 elementos.");
        }
    }
    public record TemplateView(Long id, String name, String documentType, TemplateState state, boolean deleted, Integer latestVersion) { }
    public record VersionField(Long id, String key, String visibleName, String marker, boolean required) { }
    public record VersionView(Long templateId, int number, String status, boolean staticDocument, String originalName,
            String sha256, List<VersionField> fields, String analysisJson) { }
}
