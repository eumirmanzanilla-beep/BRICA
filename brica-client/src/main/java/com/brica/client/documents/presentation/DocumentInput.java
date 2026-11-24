package com.brica.client.documents.presentation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Shared strict date parsing for documentary forms. */
public final class DocumentInput {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private DocumentInput() { }
    public static LocalDate date(String text) {
        if (text == null || text.isBlank()) { return null; }
        return text.contains("/") ? LocalDate.parse(text, DATE) : LocalDate.parse(text);
    }
    public static String format(LocalDate date) { return date == null ? "" : DATE.format(date); }
}
