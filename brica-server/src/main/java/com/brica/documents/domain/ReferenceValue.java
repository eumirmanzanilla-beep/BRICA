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

    public static ReferenceValue resolve(ReferenceField field, Object rawValue) {
        if (field.getType() == ReferenceDataType.TEXT) { return resolveText(field, rawValue); }
        if (rawValue == null || rawValue instanceof String text && text.isBlank()) {
            return new ReferenceValue(new ReferenceKey(field.getKey()).marker(), field.getKey(), field.getType(),
                    null, field.getOrigin(), ResolutionStatus.MISSING);
        }
        try {
            String value = switch (field.getType()) {
                case TEXT -> throw new IllegalStateException();
                case DATE -> {
                    var date = rawValue instanceof java.time.LocalDate d ? d : parseDate((String) rawValue);
                    yield date.getDayOfMonth() + " de " + date.getMonth().getDisplayName(java.time.format.TextStyle.FULL,
                            java.util.Locale.forLanguageTag("es-MX")) + " del " + date.getYear();
                }
                case AMOUNT -> rawValue instanceof java.math.BigDecimal decimal ? decimal.toPlainString()
                        : new java.math.BigDecimal((String) rawValue).toPlainString();
            };
            return new ReferenceValue(new ReferenceKey(field.getKey()).marker(), field.getKey(), field.getType(),
                    value, field.getOrigin(), ResolutionStatus.PRESENT);
        } catch (IllegalArgumentException | ClassCastException | java.time.DateTimeException failure) {
            return new ReferenceValue(new ReferenceKey(field.getKey()).marker(), field.getKey(), field.getType(),
                    null, field.getOrigin(), ResolutionStatus.INVALID);
        }
    }

    private static java.time.LocalDate parseDate(String text) {
        if (text.contains("/")) {
            return java.time.LocalDate.parse(text, java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT));
        }
        return java.time.LocalDate.parse(text);
    }

    public String presentationValue() {
        if (status != ResolutionStatus.PRESENT || type != ReferenceDataType.AMOUNT) { return textValue; }
        return AmountPresentation.format(new java.math.BigDecimal(textValue));
    }
}
