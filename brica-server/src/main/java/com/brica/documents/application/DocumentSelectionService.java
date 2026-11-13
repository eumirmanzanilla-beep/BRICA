package com.brica.documents.application;

import java.util.*;
import com.brica.documents.application.port.*;
import com.brica.documents.domain.*;
import com.brica.works.application.WorkNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_VALUES_READ')")
public class DocumentSelectionService {
    private final DocumentConfigurationStore store;
    private final DocumentFileStore files;
    private final ResolveReferenceValuesService values;
    public DocumentSelectionService(DocumentConfigurationStore store, DocumentFileStore files, ResolveReferenceValuesService values) {
        this.store = store; this.files = files; this.values = values;
    }
    public List<PackageOption> options(Long workId, int page, int size) {
        TemplateManagementService.pagination(page, size);
        Long client = store.workClient(workId).orElseThrow(() -> new WorkNotFoundException(workId));
        return store.packages(page, size, client, true).stream()
                .filter(p -> p.getWorkId() == null || p.getWorkId().equals(workId)).map(p -> {
            var templates = p.getTemplateIds().stream().map(id -> store.template(id, false))
                    .filter(t -> t.isEligible() && t.appliesTo(client, workId))
                    .sorted(Comparator.comparing(DocumentTemplate::getName).thenComparing(DocumentTemplate::getId))
                    .map(t -> store.latest(t.getId()).filter(this::available)
                            .map(v -> new TemplateOption(t.getId(), t.getName(), t.getDocumentType(), v.getId().number())).orElse(null))
                    .filter(Objects::nonNull).toList();
            return new PackageOption(p.getId(), p.getName(), p.getRevision(), templates);
        }).filter(option -> !option.templates().isEmpty()).toList();
    }
    public List<ResolvedDocument> resolve(Long workId, Long packageId, Long packageRevision, List<Selection> selections) {
        Long client = store.workClient(workId).orElseThrow(() -> new WorkNotFoundException(workId));
        var p = store.documentPackage(packageId, false);
        if (p.getState() != DocumentPackage.State.CONFIRMED || !Objects.equals(p.getClientId(), client)
                || p.getWorkId() != null && !p.getWorkId().equals(workId)
                || !Objects.equals(p.getRevision(), packageRevision)) { throw stale(); }
        if (selections == null || selections.isEmpty() || selections.stream().anyMatch(Objects::isNull)) {
            throw new DocumentException(400, "EMPTY_SELECTION", "Selecciona al menos una plantilla.");
        }
        Set<String> types = new HashSet<>();
        List<ResolvedDocument> result = new ArrayList<>();
        for (var selection : selections) {
            if (selection.templateId() == null || selection.version() == null) { throw stale(); }
            var t = store.template(selection.templateId(), false);
            if (!p.getTemplateIds().contains(t.getId()) || !t.isEligible() || !t.appliesTo(client, workId) || !types.add(t.getDocumentType())) { throw stale(); }
            var v = store.latest(t.getId()).orElseThrow(this::stale);
            if (!v.getId().number().equals(selection.version()) || !available(v)) { throw stale(); }
            result.add(resolveVersion(workId, t.getId(), v.getId().number()));
        }
        return List.copyOf(result);
    }
    public ResolvedDocument administrativeValues(Long workId, Long templateId, int number) {
        // This is a value-read operation, not permission to modify or download a historical template.
        return resolveVersion(workId, templateId, number);
    }
    private ResolvedDocument resolveVersion(Long workId, Long templateId, int number) {
        var version = store.version(new TemplateVersionId(templateId, number));
        var bindings = store.versionFields(version.getId());
        var required = new HashMap<Long, Boolean>();
        bindings.forEach(b -> required.put(b.getFieldId(), b.isRequired()));
        var resolved = values.resolveForWork(workId, bindings.stream().map(TemplateVersionField::getFieldId).toList());
        var result = new ArrayList<ResolvedField>();
        for (int i = 0; i < resolved.size(); i++) {
            var v = resolved.get(i);
            result.add(new ResolvedField(v.marker(), v.key(), v.type(), v.textValue(), v.presentationValue(), v.origin(),
                    required.get(bindings.get(i).getFieldId()), v.status()));
        }
        return new ResolvedDocument(templateId, number, version.isStaticDocument(), result);
    }
    private boolean available(TemplateVersion v) {
        var file = store.file(v.getFileId());
        try {
            var content = files.read(file.getRelativePath());
            return content.length == file.getSizeBytes() && DocumentHashes.sha256(content).equals(file.getSha256());
        } catch (DocumentException exception) { return false; }
    }
    private DocumentException stale() { return new DocumentException(409, "DOCUMENT_SELECTION_STALE", "La selección ya no es elegible. Consulta las opciones vigentes."); }
    public record TemplateOption(Long id, String name, String documentType, int version) { }
    public record PackageOption(Long id, String name, Long revision, List<TemplateOption> templates) { }
    public record Selection(Long templateId, Integer version) { }
    public record ResolvedField(String marker, String key, ReferenceDataType type, String textValue, String presentationValue,
            ReferenceOrigin origin, boolean required, ResolutionStatus status) { }
    public record ResolvedDocument(Long templateId, int version, boolean staticDocument, List<ResolvedField> fields) { }
}
