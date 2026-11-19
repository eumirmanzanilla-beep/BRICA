package com.brica.extraction.application;

import com.brica.documents.application.*;
import com.brica.documents.application.port.DocumentFileStore;
import com.brica.extraction.domain.ExtractionSource;
import com.brica.works.application.port.WorkAttachmentStore;
import com.brica.works.domain.WorkAttachment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class SourceEvidenceService {
    private final WorkAttachmentStore attachments; private final DocumentFileStore files;
    public SourceEvidenceService(WorkAttachmentStore attachments,DocumentFileStore files) { this.attachments=attachments; this.files=files; }
    public WorkAttachment active(Long work,Long id) {
        return attachments.find(id,work).orElseThrow(()->new DocumentException(404,"EXTRACTION_SOURCE_NOT_FOUND","Selecciona un adjunto activo de esta obra."));
    }
    public record Content(long id,String name,String kind,String sha256,byte[] bytes) { }
    public WorkAttachment metadata(ExtractionSource source) {
        return attachments.evidence(source.getAttachmentId(),source.getWorkId()).orElseThrow(()->new DocumentException(503,"EXTRACTION_SOURCE_MISSING","La evidencia original no está disponible."));
    }
    public Content read(ExtractionSource source) {
        var a=metadata(source);
        byte[] content=files.read(a.getRelativePath());
        if(content.length!=a.getSizeBytes() || !DocumentHashes.sha256(content).equals(source.getSha256()) || !a.getSha256().equals(source.getSha256())) {
            throw new DocumentException(503,"EXTRACTION_SOURCE_INTEGRITY","La evidencia no coincide con su hash registrado.");
        }
        return new Content(a.getId(),a.getOriginalName(),a.getKind().name(),a.getSha256(),content);
    }
}
