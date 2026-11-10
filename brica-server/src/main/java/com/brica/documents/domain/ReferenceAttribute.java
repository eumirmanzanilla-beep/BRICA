package com.brica.documents.domain;

import java.util.Arrays;
import java.util.Map;

import com.brica.server.shared.validation.domain.InvalidDataException;

public enum ReferenceAttribute {
    WORK_NAME(ReferenceOrigin.WORK, "Nombre de la obra"),
    CLIENT_NAME(ReferenceOrigin.CLIENT, "Nombre o razón social del cliente");

    private final ReferenceOrigin origin;
    private final String label;

    ReferenceAttribute(ReferenceOrigin origin, String label) {
        this.origin = origin;
        this.label = label;
    }

    public ReferenceOrigin getOrigin() {
        return origin;
    }

    public String getLabel() {
        return label;
    }

    public ReferenceDataType getType() {
        return ReferenceDataType.TEXT;
    }

    public String getSourcePath() {
        return "name";
    }

    public static ReferenceAttribute fromDefinition(
            ReferenceDataType type, ReferenceOrigin origin, String sourcePath) {
        return Arrays.stream(values())
                .filter(attribute -> attribute.getType() == type
                        && attribute.getOrigin() == origin
                        && attribute.getSourcePath().equals(sourcePath))
                .findFirst()
                .orElseThrow(() -> new InvalidDataException(Map.of(
                        "attribute", "El atributo no pertenece a la lista aprobada.")));
    }
}
