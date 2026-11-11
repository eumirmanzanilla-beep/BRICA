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

    protected TemplateVersion() { }
    public TemplateVersion(Long templateId, int number, Long fileId, boolean staticDocument, String analysisJson) {
        this.id = new TemplateVersionId(templateId, number);
        this.fileId = fileId;
        this.staticDocument = staticDocument;
        this.analysisJson = analysisJson;
    }
    public TemplateVersionId getId() { return id; }
    public Long getFileId() { return fileId; }
    public boolean isStaticDocument() { return staticDocument; }
    public String getAnalysisJson() { return analysisJson; }
}
