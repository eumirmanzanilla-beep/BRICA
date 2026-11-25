package com.brica.documents.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "template_versions")
public class TemplateVersion {
    @EmbeddedId
    private TemplateVersionId id;
    @Column(name = "file_id", nullable = false, updatable = false)
    private Long fileId;
    @Column(name = "static_document", nullable = false, updatable = false)
    private boolean staticDocument;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "analysis_json", nullable = false, updatable = false)
    private String analysisJson;
    @Column(nullable = false, length = 150)
    private String name;
    @Column(name = "deleted_at")
    private java.time.OffsetDateTime deletedAt;
    @Column(name = "current_version", nullable = false)
    private boolean current = true;

    protected TemplateVersion() { }
    public TemplateVersion(Long templateId, int number, Long fileId, boolean staticDocument, String analysisJson) {
        this.id = new TemplateVersionId(templateId, number);
        this.fileId = fileId;
        this.staticDocument = staticDocument;
        this.analysisJson = analysisJson;
        this.name = "Version_" + number;
    }
    public void rename(String name) {
        if (deletedAt != null) { throw new DocumentRuleException("VERSION_DELETED", "La versión eliminada es de solo lectura."); }
        var errors = new java.util.LinkedHashMap<String, String>();
        com.brica.server.shared.validation.domain.InputRules.validName(errors, "name", name, 150);
        com.brica.server.shared.validation.domain.InputRules.rejectErrors(errors);
        this.name = name;
    }
    public void delete() { if (deletedAt == null) { deletedAt = java.time.OffsetDateTime.now(); } }
    public void setCurrent(boolean current) {
        if (deletedAt != null) { throw new DocumentRuleException("VERSION_DELETED", "La versión eliminada es de solo lectura."); }
        this.current = current;
    }
    public boolean isCurrent() { return current; }
    public String getName() { return name; }
    public java.time.OffsetDateTime getDeletedAt() { return deletedAt; }
    public TemplateVersionId getId() { return id; }
    public Long getFileId() { return fileId; }
    public boolean isStaticDocument() { return staticDocument; }
    public String getAnalysisJson() { return analysisJson; }
}
