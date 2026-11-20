package com.brica.documents.domain;

import java.util.List;

public record DocxAnalysis(List<Occurrence> occurrences, List<Diagnostic> diagnostics, boolean staticDocument) {
    public DocxAnalysis {
        occurrences = List.copyOf(occurrences);
        diagnostics = List.copyOf(diagnostics);
    }

    public record Occurrence(String key, String marker, String location) { }
    public record Diagnostic(String code, String location, String message) { }
}
