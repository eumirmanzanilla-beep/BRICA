package com.brica.documents.domain;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import com.brica.server.shared.validation.domain.InputRules;
import com.brica.server.shared.validation.domain.InvalidDataException;
import jakarta.persistence.*;

@Entity
@Table(name = "document_templates")
public class DocumentTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_template")
    private Long id;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "document_type", nullable = false, length = 100)
    private String documentType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TemplateState state;
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    @Version @Column(nullable = false)
    private Long revision;
    @Column(name = "client_id", updatable = false)
    private Long clientId;
    @Column(name = "work_id", updatable = false)
    private Long workId;

    protected DocumentTemplate() { }
    public DocumentTemplate(String name, String documentType, TemplateState state) {
        change(name, documentType, state);
    }
    public DocumentTemplate(String name, String documentType, TemplateState state, Long clientId, Long workId) {
        this(name, documentType, state); this.clientId = clientId; this.workId = workId;
    }
    public Long getClientId() { return clientId; }
    public Long getWorkId() { return workId; }
    public boolean appliesTo(Long client, Long work) {
        return (clientId == null || clientId.equals(client)) && (workId == null || workId.equals(work));
    }
    public void change(String name, String documentType, TemplateState state) {
        requireEditable();
        var errors = new LinkedHashMap<String, String>();
        InputRules.requiredText(errors, "name", name, 150);
        InputRules.requiredText(errors, "documentType", documentType, 100);
        if (state == null) { throw new InvalidDataException(Map.of("state", "Selecciona un estado válido.")); }
        InputRules.rejectErrors(errors);
        this.name = name;
        this.documentType = documentType;
        this.state = state;
    }
    public void requireEditable() {
        if (deletedAt != null) { throw new DocumentRuleException("TEMPLATE_DELETED", "La plantilla eliminada es de solo lectura."); }
    }
    public void delete() { requireEditable(); deletedAt = OffsetDateTime.now(); }
    public boolean isEligible() { return deletedAt == null && state == TemplateState.CURRENT; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDocumentType() { return documentType; }
    public TemplateState getState() { return state; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public Long getRevision() { return revision; }
}
