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
    private final DocumentContextService context;
    private final PackageReadiness readiness;
    public TemplateManagementService(DocumentConfigurationStore store, DocumentFileStore files, ReferenceFieldStore fields, DocumentContextService context, PackageReadiness readiness) {
        this.store = store; this.files = files; this.fields = fields; this.context = context; this.readiness = readiness;
    }
    public TemplateView create(String name, String type, TemplateState state) {
        return create(name, type, state, null, null);
    }
    public TemplateView create(String name, String type, TemplateState state, Long clientId, Long workId) {
        Long client = context.client(clientId, workId);
        var template = new DocumentTemplate(name, type, state, client, workId);
        store.add(template); store.flush();
        return view(template);
    }
    public TemplateView update(Long id, String name, String type, TemplateState state) {
        var template = store.template(id, true);
        template.change(name, type, state);
        readiness.templateChanged(id); store.flush();
        return view(template);
    }
    public void delete(Long id) { store.template(id, true).delete(); readiness.templateChanged(id); }
    @Transactional(readOnly = true)
    public TemplateView get(Long id) { InputRules.positiveId("templateId", id); return view(store.template(id, false)); }
    @Transactional(readOnly = true)
    public List<TemplateView> list(int page, int size) {
        return list(page, size, false);
    }
    @Transactional(readOnly = true)
    public List<TemplateView> list(int page, int size, boolean includeDeleted) {
        pagination(page, size);
        var templates = store.templates(page, size, includeDeleted);
        var latest = store.latestNumbers(templates.stream().map(DocumentTemplate::getId).toList());
        return templates.stream().map(t -> new TemplateView(t.getId(), t.getName(), t.getDocumentType(), t.getState(),
                t.getDeletedAt() != null, latest.get(t.getId()), t.getClientId(), t.getWorkId())).toList();
    }
    @Transactional(readOnly = true)
    public List<TemplateView> list(int page, int size, boolean includeDeleted, Long clientId, Long workId) {
        if (clientId == null && workId == null) { return list(page, size, includeDeleted); }
        pagination(page, size);
        Long client = context.client(clientId, workId);
        var result = store.scopedTemplates(page, size, includeDeleted, client, workId);
        var latest = store.latestNumbers(result.stream().map(DocumentTemplate::getId).toList());
        return result.stream().map(t -> new TemplateView(t.getId(), t.getName(), t.getDocumentType(), t.getState(),
                t.getDeletedAt() != null, latest.get(t.getId()), t.getClientId(), t.getWorkId())).toList();
    }
    @Transactional(readOnly = true)
    public List<VersionView> versions(Long templateId) {
        store.template(templateId, false);
        return store.versions(templateId).stream().map(this::versionView).toList();
    }
    public VersionView renameVersion(Long templateId, int number, String name) {
        store.template(templateId, true).requireEditable();
        var version = store.version(new TemplateVersionId(templateId, number));
        version.rename(name); store.flush(); return versionView(version);
    }
    public VersionView changeVersionAvailability(Long templateId, int number, boolean current) {
        store.template(templateId, true).requireEditable();
        var version = store.version(new TemplateVersionId(templateId, number));
        version.setCurrent(current); readiness.templateChanged(templateId); store.flush(); return versionView(version);
    }
    public void deleteVersion(Long templateId, int number) {
        store.template(templateId, true).requireEditable();
        store.version(new TemplateVersionId(templateId, number)).delete(); readiness.templateChanged(templateId);
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
                store.latest(t.getId()).map(v -> v.getId().number()).orElse(null), t.getClientId(), t.getWorkId());
    }
    private VersionView versionView(TemplateVersion v) {
        var bindings = store.versionFields(v.getId());
        var byId = fields.findAllById(bindings.stream().map(TemplateVersionField::getFieldId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(ReferenceField::getId, java.util.function.Function.identity()));
        var details = bindings.stream().map(b -> new VersionField(b.getFieldId(), byId.get(b.getFieldId()).getKey(),
                byId.get(b.getFieldId()).getVisibleName(), b.getMarker(), b.isRequired())).toList();
        var file = store.file(v.getFileId());
        return new VersionView(v.getId().templateId(), v.getId().number(), "COMPLETED", v.isStaticDocument(),
                file.getOriginalName(), file.getSha256(), details, v.getAnalysisJson(), v.getName(), v.getDeletedAt() != null, v.isCurrent());
    }
    public static void pagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new DocumentException(400, "INVALID_PAGINATION", "Usa una página positiva y entre 1 y 100 elementos.");
        }
    }
    public record TemplateView(Long id, String name, String documentType, TemplateState state, boolean deleted, Integer latestVersion, Long clientId, Long workId) { }
    public record VersionField(Long id, String key, String visibleName, String marker, boolean required) { }
    public record VersionView(Long templateId, int number, String status, boolean staticDocument, String originalName,
            String sha256, List<VersionField> fields, String analysisJson, String name, boolean deleted, boolean current) { }
}
