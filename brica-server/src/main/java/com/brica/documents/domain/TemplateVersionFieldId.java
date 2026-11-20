package com.brica.documents.domain;

import java.io.Serializable;
import jakarta.persistence.*;

@Embeddable
public record TemplateVersionFieldId(@Column(name = "template_id") Long templateId,
        @Column(name = "version_number") Integer number,
        @Column(name = "field_id") Long fieldId) implements Serializable { }
