package com.brica.documents.domain;

import java.util.LinkedHashMap;
import java.util.Map;

import com.brica.server.shared.validation.domain.InputRules;
import com.brica.server.shared.validation.domain.InvalidDataException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "reference_fields")
public class ReferenceField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reference_field")
    private Long id;

    @Column(name = "key", nullable = false, length = 50, updatable = false)
    private String key;

    @Column(name = "visible_name", nullable = false, length = 50)
    private String visibleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 30, updatable = false)
    private ReferenceDataType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 100, updatable = false)
    private ReferenceOrigin origin;

    @Column(name = "source_path", nullable = false, length = 150, updatable = false)
    private String sourcePath;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;

    protected ReferenceField() {
    }

    public ReferenceField(String key, String visibleName, ReferenceAttribute attribute) {
        this.key = new ReferenceKey(key).value();
        if (attribute == null) {
            throw new InvalidDataException(Map.of("attribute", "Selecciona un atributo aprobado."));
        }
        rename(visibleName);
        this.type = attribute.getType();
        this.origin = attribute.getOrigin();
        this.sourcePath = attribute.getSourcePath();
    }

    public static ReferenceField additional(String key, String visibleName, ReferenceDataType type) {
        var field = new ReferenceField();
        field.key = new ReferenceKey(key).value();
        field.rename(visibleName);
        if (type == null) {
            throw new InvalidDataException(Map.of("type", "Selecciona texto, fecha o importe."));
        }
        field.type = type;
        field.origin = ReferenceOrigin.ADDITIONAL;
        field.sourcePath = field.key;
        return field;
    }

    public void rename(String visibleName) {
        var errors = new LinkedHashMap<String, String>();
        InputRules.requiredText(errors, "visibleName", visibleName, 50);
        InputRules.rejectErrors(errors);
        this.visibleName = visibleName;
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getVisibleName() {
        return visibleName;
    }

    public ReferenceDataType getType() {
        return type;
    }

    public ReferenceOrigin getOrigin() {
        return origin;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public ReferenceAttribute getAttribute() {
        return ReferenceAttribute.fromDefinition(type, origin, sourcePath);
    }

    public Long getRevision() {
        return revision;
    }
}
