package com.brica.client.documents.api;

import java.util.List;

public final class DocumentDtos {
    private DocumentDtos() { }
    public enum TemplateState {
        CURRENT("Vigente"), DRAFT("Borrador"), UNAVAILABLE("No vigente");
        private final String label;
        TemplateState(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
    public record Attribute(String id, String label, String type, String origin, String sourcePath) {
        @Override public String toString() { return label; }
    }
    public record Field(Long id, String key, String visibleName, String type, String origin, String sourcePath) {
        @Override public String toString() { return key + " — " + visibleName; }
    }
    public record Template(Long id, String name, String documentType, TemplateState state, boolean deleted, Integer latestVersion, Long clientId, Long workId) {
        public Template(Long id, String name, String documentType, TemplateState state, boolean deleted, Integer latestVersion) {
            this(id, name, documentType, state, deleted, latestVersion, null, null);
        }
        @Override public String toString() { return name + " · " + (deleted ? "Eliminada" : state) + " · " + documentType; }
    }
    public record VersionField(Long id, String key, String visibleName, String marker, boolean required) { }
    public record Version(Long templateId, int number, String status, boolean staticDocument, String originalName,
            String sha256, List<VersionField> fields, String analysisJson, String name, boolean deleted, boolean current) {
        @Override public String toString() { return name + " · " + (current ? "Vigente" : "No vigente") + (staticDocument ? " · Documento estático" : ""); }
    }
    public record DetectedField(Long id, String key, String visibleName, String type, String origin,
            String sourcePath, Boolean proposedRequired) { }
    public record Occurrence(String key, String marker, String location) { }
    public record Diagnostic(String code, String location, String marker, String message) { }
    public record Review(String fingerprint, List<DetectedField> fields, List<Occurrence> occurrences,
            List<Diagnostic> diagnostics, boolean staticDocument) { }
    public record Operation(String id, Long templateId, String status, Integer versionNumber, String failure) { }
    public record PackageTemplate(Long id, String name, TemplateState state, boolean deleted) { }
    public record Package(Long id, Long clientId, String name, String state, Long revision, List<PackageTemplate> templates, Long workId, boolean ready) {
        public Package(Long id, Long clientId, String name, String state, Long revision, List<PackageTemplate> templates) {
            this(id, clientId, name, state, revision, templates, null, false);
        }
        public Package(Long id, Long clientId, String name, String state, Long revision, List<PackageTemplate> templates, Long workId) {
            this(id, clientId, name, state, revision, templates, workId, false);
        }
        @Override public String toString() { return (name == null || name.isBlank() ? "Sin nombre" : name) + (ready ? " · Listo" : " · No listo") + (workId == null ? " · Cliente" : " · Obra #" + workId); }
    }
    public record CreatedPackageTemplate(Package documentPackage, Template template) { }
    public record TemplateOption(Long id, String name, String documentType, int version) {
        @Override public String toString() { return name + " · v" + version; }
    }
    public record PackageOption(Long id, String name, Long revision, List<TemplateOption> templates) {
        @Override public String toString() { return name; }
    }
    public record ResolvedField(String marker, String key, String type, String textValue, String presentationValue, String origin, boolean required, String status) { }
    public record ResolvedDocument(Long templateId, int version, boolean staticDocument, List<ResolvedField> fields) { }
}
