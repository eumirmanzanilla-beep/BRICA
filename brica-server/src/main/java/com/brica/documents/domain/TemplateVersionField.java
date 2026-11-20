package com.brica.documents.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "template_version_fields")
public class TemplateVersionField {
    @EmbeddedId
    private TemplateVersionFieldId id;
    @Column(nullable = false, updatable = false)
    private boolean required;
    @Column(nullable = false, length = 54, updatable = false)
    private String marker;

    protected TemplateVersionField() { }
    public TemplateVersionField(TemplateVersionId version, ReferenceField field, boolean required) {
        this.id = new TemplateVersionFieldId(version.templateId(), version.number(), field.getId());
        this.required = required;
        this.marker = new ReferenceKey(field.getKey()).marker();
    }
    public Long getFieldId() { return id.fieldId(); }
    public boolean isRequired() { return required; }
    public String getMarker() { return marker; }
}
