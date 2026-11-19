package com.brica.works.application;

import java.util.List;
import java.util.UUID;
import com.brica.documents.application.*;
import com.brica.documents.application.port.DocumentFileStore;
import com.brica.works.application.port.*;
import com.brica.works.domain.WorkAttachment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkAttachmentService {
    private final WorkStore works;
    private final WorkAttachmentStore store;
    private final DocumentFileStore files;
    public WorkAttachmentService(WorkStore works, WorkAttachmentStore store, DocumentFileStore files) {
        this.works = works; this.store = store; this.files = files;
    }
    private void work(Long id) {
        var work = works.findById(id).orElseThrow(() -> new WorkNotFoundException(id));
        if (work.getClient().getDeletedAt() != null) { throw new WorkNotFoundException(id); }
    }
    @PreAuthorize("@accessPolicy.hasPermission('WORKS_WRITE')")
    public View upload(Long workId, WorkAttachment.Kind kind, String filename, byte[] content) {
        work(workId);
        if (kind == null || content == null || content.length == 0 || content.length > 10 * 1024 * 1024) {
            throw new DocumentException(400, "INVALID_WORK_ATTACHMENT", "Selecciona tipo y archivo de hasta 10 MiB.");
        }
        String mime = AttachmentFormat.mime(filename, content);
        String safeName = filename.replace('\\', '/'); safeName = safeName.substring(safeName.lastIndexOf('/') + 1);
        var key = UUID.randomUUID(); files.put(key, content);
        return View.from(store.save(new WorkAttachment(workId, kind, safeName, mime, files.relativePath(key),
                content.length, DocumentHashes.sha256(content))));
    }
    @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ')")
    @Transactional(readOnly = true)
    public List<View> list(Long workId, int page, int size) {
        work(workId); TemplateManagementService.pagination(page, size);
        return store.list(workId, page, size).stream().map(View::from).toList();
    }
    @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ')")
    @Transactional(readOnly = true)
    public Download download(Long workId, Long id) {
        work(workId);
        var value = store.find(id, workId).orElseThrow(() -> new DocumentException(404, "WORK_ATTACHMENT_NOT_FOUND", "El archivo no pertenece a esta obra."));
        byte[] content = files.read(value.getRelativePath());
        if (content.length != value.getSizeBytes() || !DocumentHashes.sha256(content).equals(value.getSha256())) {
            throw new DocumentException(503, "DOCUMENT_INTEGRITY_ERROR", "No se pudo verificar la integridad del archivo.");
        }
        return new Download(value.getOriginalName(), value.getMimeType(), content);
    }
    public record View(Long id, String kind, String originalName, String mimeType, long sizeBytes, String sha256) {
        public static View from(WorkAttachment value) { return new View(value.getId(), value.getKind().name(), value.getOriginalName(),
                value.getMimeType(), value.getSizeBytes(), value.getSha256()); }
    }
    public record Download(String name, String mime, byte[] content) { }
    @PreAuthorize("@accessPolicy.hasPermission('WORKS_WRITE')")
    public void delete(Long workId, Long id) {
        work(workId);
        store.find(id, workId).orElseThrow(() -> new DocumentException(404, "WORK_ATTACHMENT_NOT_FOUND", "El archivo no pertenece a esta obra o está eliminado.")).delete();
    }
}
