package com.brica.documents.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.brica.documents.application.port.*;
import com.brica.documents.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_CONFIG_ADMIN')")
public class TemplateAnalysisService {
    private final DocxAnalyzer analyzer;
    private final ReferenceFieldStore fields;
    private final DocumentConfigurationStore documents;
    private final JsonMapper json;

    public TemplateAnalysisService(DocxAnalyzer analyzer, ReferenceFieldStore fields,
            DocumentConfigurationStore documents, JsonMapper json) {
        this.analyzer = analyzer;
        this.fields = fields;
        this.documents = documents;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public Review analyze(Long templateId, byte[] content) {
        var template = documents.template(templateId, false);
        template.requireEditable();
        var analysis = analyzer.analyze(content);
        var diagnostics = new ArrayList<>(analysis.diagnostics());
        var keys = analysis.occurrences().stream().map(DocxAnalysis.Occurrence::key).distinct().sorted().toList();
        var definitions = fields.findAllByKey(keys).stream().sorted(Comparator.comparing(ReferenceField::getKey)).toList();
        var known = definitions.stream().map(ReferenceField::getKey).collect(Collectors.toSet());
        analysis.occurrences().stream().filter(o -> !known.contains(o.key())).forEach(o ->
                diagnostics.add(new DocxAnalysis.Diagnostic("UNKNOWN_MARKER", o.location(), o.marker(),
                        "Clave no registrada: " + o.key())));
        Map<Long, Boolean> previous = documents.latest(templateId).map(v -> documents.versionFields(v.getId())
                .stream().collect(Collectors.toMap(TemplateVersionField::getFieldId, TemplateVersionField::isRequired)))
                .orElseGet(Map::of);
        var detected = definitions.stream().map(f -> new DetectedField(f.getId(), f.getKey(), f.getVisibleName(),
                f.getType(), f.getOrigin(), f.getSourcePath(), previous.get(f.getId()))).toList();
        // Include descriptive changes and previous requiredness: any revised proposal must be reviewed again.
        String fingerprint = DocumentHashes.sha256(json.writeValueAsString(List.of(
                DocumentHashes.sha256(content), templateId, detected, analysis)));
        return new Review(fingerprint, detected, analysis.occurrences(), diagnostics, analysis.staticDocument());
    }

    public record DetectedField(Long id, String key, String visibleName, ReferenceDataType type,
            ReferenceOrigin origin, String sourcePath, Boolean proposedRequired) { }
    public record Review(String fingerprint, List<DetectedField> fields, List<DocxAnalysis.Occurrence> occurrences,
            List<DocxAnalysis.Diagnostic> diagnostics, boolean staticDocument) {
        public boolean valid() { return diagnostics.isEmpty(); }
    }
}
