package com.brica.documents.domain;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record TemplateVersionId(@Column(name = "template_id") Long templateId,
        @Column(name = "version_number") Integer number) implements Serializable { }
