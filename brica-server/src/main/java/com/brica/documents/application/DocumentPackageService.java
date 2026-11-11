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
    public DocumentPackageService(DocumentConfigurationStore store) { this.store = store; }

    public PackageView create(Long clientId, String name, List<Long> templates, boolean confirm) {
        var value = new DocumentPackage();
        edit(value, clientId, name, templates, confirm);
        store.add(value); store.flush();
        return view(value);
    }
    public PackageView update(Long id, String expected, Long clientId, String name, List<Long> templates, boolean confirm) {
        var value = store.documentPackage(id, true);
        precondition(value, expected);
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
    @Transactional(readOnly = true)
    public PackageView get(Long id) { return view(store.documentPackage(id, false)); }
    @Transactional(readOnly = true)
    public List<PackageView> list(int page, int size) {
        TemplateManagementService.pagination(page, size);
        return store.packages(page, size, null, false).stream().map(this::view).toList();
    }
    private void edit(DocumentPackage value, Long client, String name, List<Long> templates, boolean confirm) {
        value.edit(client, name, templates, confirm);
        if (client != null && !store.activeClient(client)) {
            throw new DocumentException(400, "PACKAGE_CLIENT_UNAVAILABLE", "Selecciona un cliente vigente.");
        }
        for (Long id : templates.stream().sorted().toList()) {
            var t = store.template(id, true);
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
        return new PackageView(value.getId(), value.getClientId(), value.getName(), value.getState(), value.getRevision(), templates);
    }
    public static String etag(Long revision) { return "\"" + revision + "\""; }
    public record PackageTemplate(Long id, String name, TemplateState state, boolean deleted) { }
    public record PackageView(Long id, Long clientId, String name, DocumentPackage.State state, Long revision, List<PackageTemplate> templates) { }
}
