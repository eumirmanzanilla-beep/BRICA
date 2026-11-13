package com.brica.documents.domain;

import java.util.Arrays;
import java.util.Map;
import com.brica.server.shared.validation.domain.InvalidDataException;

public enum ReferenceAttribute {
    WORK_NAME(ReferenceOrigin.WORK, "Nombre de la obra", ReferenceDataType.TEXT, "name"),
    CLIENT_NAME(ReferenceOrigin.CLIENT, "Nombre o razón social del cliente", ReferenceDataType.TEXT, "name"),
    WORK_ID(ReferenceOrigin.WORK, "Identificador de obra", ReferenceDataType.TEXT, "id"),
    WORK_CLIENT_ID(ReferenceOrigin.WORK, "Identificador del cliente de la obra", ReferenceDataType.TEXT, "client.id"),
    CLIENT_ID(ReferenceOrigin.CLIENT, "Identificador de cliente", ReferenceDataType.TEXT, "id"),
    CLIENT_LEGAL_REPRESENTATIVE(ReferenceOrigin.CLIENT, "Representante legal", ReferenceDataType.TEXT, "legalRepresentative"),
    WORK_CONTRACT_NUMBER(ReferenceOrigin.WORK, "Número de contrato", ReferenceDataType.TEXT, "contractNumber"),
    WORK_CONTRACTOR(ReferenceOrigin.WORK, "Contratista", ReferenceDataType.TEXT, "contractor"),
    WORK_LOCATION(ReferenceOrigin.WORK, "Ubicación", ReferenceDataType.TEXT, "location"),
    WORK_DESCRIPTION(ReferenceOrigin.WORK, "Descripción de los trabajos", ReferenceDataType.TEXT, "description"),
    WORK_STATUS(ReferenceOrigin.WORK, "Estado de la obra", ReferenceDataType.TEXT, "status"),
    WORK_START_DATE(ReferenceOrigin.WORK, "Fecha de inicio", ReferenceDataType.DATE, "startDate"),
    WORK_END_DATE(ReferenceOrigin.WORK, "Fecha de terminación contractual", ReferenceDataType.DATE, "endDate"),
    WORK_ACTUAL_END_DATE(ReferenceOrigin.WORK, "Fecha de terminación real", ReferenceDataType.DATE, "actualEndDate"),
    WORK_CONTRACTED_AMOUNT(ReferenceOrigin.WORK, "Monto contratado", ReferenceDataType.AMOUNT, "contractedAmount"),
    WORK_AMOUNT_BEFORE_TAX(ReferenceOrigin.WORK, "Monto antes de IVA", ReferenceDataType.AMOUNT, "amountBeforeTax"),
    WORK_TAX_AMOUNT(ReferenceOrigin.WORK, "IVA", ReferenceDataType.AMOUNT, "taxAmount");

    private final ReferenceOrigin origin;
    private final String label;
    private final ReferenceDataType type;
    private final String sourcePath;
    ReferenceAttribute(ReferenceOrigin origin, String label, ReferenceDataType type, String sourcePath) {
        this.origin = origin; this.label = label; this.type = type; this.sourcePath = sourcePath;
    }
    public ReferenceOrigin getOrigin() { return origin; }
    public String getLabel() { return label; }
    public ReferenceDataType getType() { return type; }
    public String getSourcePath() { return sourcePath; }
    public String canonicalKey() {
        return switch (this) {
            case WORK_NAME -> "NOMBRE_OBRA"; case CLIENT_NAME -> "NOMBRE_CLIENTE";
            case WORK_ID -> "ID_OBRA"; case WORK_CLIENT_ID -> "ID_CLIENTE_OBRA"; case CLIENT_ID -> "ID_CLIENTE";
            case CLIENT_LEGAL_REPRESENTATIVE -> "REPRESENTANTE_LEGAL"; case WORK_CONTRACT_NUMBER -> "NUMERO_CONTRATO";
            case WORK_CONTRACTOR -> "CONTRATISTA"; case WORK_LOCATION -> "UBICACION";
            case WORK_DESCRIPTION -> "DESCRIPCION_TRABAJO"; case WORK_STATUS -> "ESTADO_OBRA";
            case WORK_START_DATE -> "FECHA_INICIO"; case WORK_END_DATE -> "FECHA_TERMINACION";
            case WORK_ACTUAL_END_DATE -> "FECHA_TERMINACION_REAL"; case WORK_CONTRACTED_AMOUNT -> "MONTO_CONTRATADO";
            case WORK_AMOUNT_BEFORE_TAX -> "MONTO_ANTES_IVA"; case WORK_TAX_AMOUNT -> "IVA";
        };
    }
    public static java.util.Optional<ReferenceAttribute> forKey(String key) {
        return Arrays.stream(values()).filter(a -> a.canonicalKey().equals(key)).findFirst();
    }
    public static ReferenceAttribute fromDefinition(ReferenceDataType type, ReferenceOrigin origin, String sourcePath) {
        return Arrays.stream(values()).filter(a -> a.type == type && a.origin == origin && a.sourcePath.equals(sourcePath))
                .findFirst().orElseThrow(() -> new InvalidDataException(Map.of("attribute", "El atributo no pertenece a la lista aprobada.")));
    }
}
