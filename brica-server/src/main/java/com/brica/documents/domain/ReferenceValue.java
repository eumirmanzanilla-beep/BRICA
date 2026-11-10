package com.brica.documents.domain;

public record ReferenceValue(
        String marker,
        String key,
        ReferenceDataType type,
        String textValue,
        ReferenceOrigin origin,
        ResolutionStatus status) {

    public static ReferenceValue resolveText(ReferenceField field, Object rawValue) {
        ResolutionStatus status;
        String textValue = null;
        if (rawValue == null || rawValue instanceof String text && text.isBlank()) {
            status = ResolutionStatus.MISSING;
        } else if (rawValue instanceof String text) {
            status = ResolutionStatus.PRESENT;
            textValue = text;
        } else {
            status = ResolutionStatus.INVALID;
        }
        return new ReferenceValue(new ReferenceKey(field.getKey()).marker(), field.getKey(),
                field.getType(), textValue, field.getOrigin(), status);
    }
}
