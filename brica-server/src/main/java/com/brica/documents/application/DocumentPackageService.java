package com.brica.documents.application;

import java.util.Comparator;
import java.util.List;
import com.brica.documents.application.port.DocumentConfigurationStore;
import com.brica.documents.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN')")
public class DocumentPackageService {
    private final DocumentConfigurationStore store;
    private final DocumentContextService context;
    public DocumentPackageService(DocumentConfigurationStore store, DocumentContextService context) { this.store = store; this.context = context; }

    @PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN') and @accessPolicy.hasPermission('WORKS_READ')")
    public PackageView createFromWork(Long workId, String name, List<Long> templates, boolean confirm) {
        var value = new DocumentPackage(); value.scope(workId);
        edit(value, clientOfWork(workId), name, templates, confirm);
        store.add(value); store.flush(); return view(value);
    }
    @PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN') and @accessPolicy.hasPermission('WORKS_READ')")
    public PackageView updateFromWork(Long id, String expected, Long workId, String name, List<Long> templates, boolean confirm) {
        var value = store.documentPackage(id, true); precondition(value, expected);
        value.scope(workId); edit(value, clientOfWork(workId), name, templates, confirm);
        store.flush(); return view(value);
    }
    private Long clientOfWork(Long workId) {
        com.brica.server.shared.validation.domain.InputRules.positiveId("workId", workId);
        return store.workClient(workId).orElseThrow(() -> new DocumentException(404,
                "WORK_NOT_FOUND", "Selecciona una obra vigente con un cliente vigente."));
    }

    public PackageView create(Long clientId, String name, List<Long> templates, boolean confirm) {
        var value = new DocumentPackage();
        edit(value, clientId, name, templates, confirm);
        store.add(value); store.flush();
        return view(value);
    }
    public PackageView update(Long id, String expected, Long clientId, String name, List<Long> templates, boolean confirm) {
        var value = store.documentPackage(id, true);
        precondition(value, expected);
        // Omitting workId while editing an existing work package must not widen its scope.
        edit(value, clientId, name, templates, confirm);
        store.flush();
        return view(value);
    }
    public void discard(Long id, String expected) {
        var value = store.documentPackage(id, true);
        precondition(value, expected);
        if (value.getState() != DocumentPackage.State.DRAFT) {
            throw new DocumentException(409, "PACKAGE_NOT_DRAFT", "Solo se pueden descartar borradores.");
        }
        store.discard(value);
    }
    public CreatedTemplate createTemplate(Long id, String expected, String name, String type) {
        var value = store.documentPackage(id, true); precondition(value, expected);
        if (value.getClientId() == null) { throw new DocumentException(400, "PACKAGE_CLIENT_REQUIRED", "Guarda primero el paquete en una obra o cliente."); }
        context.client(value.getClientId(), value.getWorkId());
        var template = new DocumentTemplate(name, type, TemplateState.CURRENT, value.getClientId(), value.getWorkId());
        store.add(template); store.flush();
        var ids = new java.util.ArrayList<>(value.getTemplateIds()); ids.add(template.getId());
        // Do not invalidate a confirmed package while its new template is still unversioned.
        if (value.getState() == DocumentPackage.State.DRAFT) {
            edit(value, value.getClientId(), value.getName(), ids, false); store.flush();
        }
        return new CreatedTemplate(view(value), new TemplateManagementService.TemplateView(template.getId(), template.getName(),
                template.getDocumentType(), template.getState(), false, null, template.getClientId(), template.getWorkId()));
    }
    public record CreatedTemplate(PackageView documentPackage, TemplateManagementService.TemplateView template) { }
    @Transactional(readOnly = true)
    public PackageView get(Long id) { return view(store.documentPackage(id, false)); }
    @Transactional(readOnly = true)
    public List<PackageView> list(int page, int size) {
        TemplateManagementService.pagination(page, size);
        return store.packages(page, size, null, false).stream().map(this::view).toList();
    }
    @Transactional(readOnly = true)
    @PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN') and @accessPolicy.hasPermission('WORKS_READ')")
    public List<PackageView> listForWork(Long workId, int page, int size) {
        TemplateManagementService.pagination(page, size);
        return store.scopedPackages(page, size, clientOfWork(workId), workId).stream().map(this::view).toList();
    }
    @Transactional(readOnly = true)
    @PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN') and @accessPolicy.hasPermission('CLIENTS_READ')")
    public List<PackageView> listForClient(Long clientId, int page, int size) {
        com.brica.server.shared.validation.domain.InputRules.positiveId("clientId", clientId);
        TemplateManagementService.pagination(page, size);
        if (!store.activeClient(clientId)) { throw new DocumentException(404, "CLIENT_NOT_FOUND", "Selecciona un cliente vigente."); }
        return store.scopedPackages(page, size, clientId, null).stream().map(this::view).toList();
    }
    private void edit(DocumentPackage value, Long client, String name, List<Long> templates, boolean confirm) {
        value.edit(client, name, templates, confirm);
        if (client != null && !store.activeClient(client)) {
            throw new DocumentException(400, "PACKAGE_CLIENT_UNAVAILABLE", "Selecciona un cliente vigente.");
        }
        for (Long id : templates.stream().sorted().toList()) {
            var t = store.template(id, true);
            if (!t.appliesTo(client, value.getWorkId())) {
                throw new DocumentException(409, "TEMPLATE_SCOPE_MISMATCH", "La plantilla pertenece a otra obra o cliente.");
            }
            if (t.getDeletedAt() != null || value.getState() == DocumentPackage.State.CONFIRMED
                    && (!t.isEligible() || store.latest(id).isEmpty())) {
                throw new DocumentException(409, "TEMPLATE_NOT_ELIGIBLE", "El paquete confirmado requiere plantillas vigentes con una versión completa.");
            }
        }
    }
    private void precondition(DocumentPackage value, String expected) {
        if (expected == null) { throw new DocumentException(428, "PRECONDITION_REQUIRED", "Consulta el paquete y envía su ETag en If-Match."); }
        if (!etag(value.getRevision()).equals(expected)) {
            throw new DocumentException(412, "STALE_PACKAGE", "Otro usuario modificó el paquete. Recarga y revisa tus cambios.");
        }
    }
    private PackageView view(DocumentPackage value) {
        var templates = value.getTemplateIds().stream().map(id -> store.template(id, false))
                .sorted(Comparator.comparing(DocumentTemplate::getName).thenComparing(DocumentTemplate::getId))
                .map(t -> new PackageTemplate(t.getId(), t.getName(), t.getState(), t.getDeletedAt() != null)).toList();
        return new PackageView(value.getId(), value.getClientId(), value.getName(), value.getState(), value.getRevision(), templates, value.getWorkId());
    }
    public static String etag(Long revision) { return "\"" + revision + "\""; }
    public record PackageTemplate(Long id, String name, TemplateState state, boolean deleted) { }
    public record PackageView(Long id, Long clientId, String name, DocumentPackage.State state, Long revision, List<PackageTemplate> templates, Long workId) { }
}
