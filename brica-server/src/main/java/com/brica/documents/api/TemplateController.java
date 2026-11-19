package com.brica.documents.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.brica.documents.application.*;
import com.brica.documents.domain.TemplateState;
import com.brica.server.shared.error.api.ApiProblems;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class TemplateController {
    private final TemplateManagementService templates;
    private final TemplateAnalysisService analysis;
    private final RegisterTemplateVersionService versions;
    private final DocumentSelectionService selections;
    public TemplateController(TemplateManagementService templates, TemplateAnalysisService analysis,
            RegisterTemplateVersionService versions, DocumentSelectionService selections) {
        this.templates = templates; this.analysis = analysis; this.versions = versions; this.selections = selections;
    }
    @GetMapping("/document-templates")
    public List<TemplateManagementService.TemplateView> list(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted,
            @RequestParam(name = "clientId", required = false) Long clientId,
            @RequestParam(name = "workId", required = false) Long workId) {
        return templates.list(page, size, includeDeleted, clientId, workId);
    }
    @GetMapping("/document-templates/{id}")
    public TemplateManagementService.TemplateView get(@PathVariable("id") Long id) { return templates.get(id); }
    @PostMapping("/document-templates") @ResponseStatus(HttpStatus.CREATED)
    public TemplateManagementService.TemplateView create(@Valid @RequestBody TemplateRequest request) {
        return templates.create(request.name(), request.documentType(), request.state(), request.clientId(), request.workId());
    }
    @PutMapping("/document-templates/{id}")
    public TemplateManagementService.TemplateView update(@PathVariable("id") Long id, @Valid @RequestBody TemplateRequest request) {
        return templates.update(id, request.name(), request.documentType(), request.state());
    }
    @DeleteMapping("/document-templates/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) { templates.delete(id); }
    @PostMapping(value = "/document-templates/{id}/analyses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TemplateAnalysisService.Review analyze(@PathVariable("id") Long id, @RequestPart("file") MultipartFile file) throws IOException {
        return analysis.analyze(id, file.getBytes());
    }
    @PostMapping(value = "/document-templates/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> confirm(@PathVariable("id") Long id, @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") ConfirmationRequest request) throws IOException {
        var result = versions.confirm(id, request.confirmationId(), file.getOriginalFilename(), file.getBytes(),
                request.fingerprint(), request.required(), request.staticAccepted(), request.name());
        if (result.updatedAnalysis() != null) {
            var problem = ApiProblems.problem(HttpStatus.CONFLICT, "TEMPLATE_REVIEW_CHANGED", "Revisa el análisis actualizado",
                    "Cambió el archivo o su configuración. Revisa y confirma nuevamente.", "Comprueba campos y obligatoriedad.");
            problem.setProperty("updatedAnalysis", result.updatedAnalysis());
            return ResponseEntity.status(409).body(problem);
        }
        return ResponseEntity.ok(result.operation());
    }
    @GetMapping("/document-templates/{id}/versions")
    public List<TemplateManagementService.VersionView> versions(@PathVariable("id") Long id) { return templates.versions(id); }
    @GetMapping("/document-templates/{id}/versions/{number}")
    public TemplateManagementService.VersionView version(@PathVariable("id") Long id, @PathVariable("number") int number) {
        return templates.version(id, number);
    }
    @GetMapping("/document-templates/{id}/versions/{number}/file")
    public ResponseEntity<byte[]> download(@PathVariable("id") Long id, @PathVariable("number") int number) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template-" + id + "-" + number + ".docx")
                .body(templates.download(id, number));
    }
    @PostMapping("/document-templates/{id}/versions/{number}/values")
    public DocumentSelectionService.ResolvedDocument values(@PathVariable("id") Long id, @PathVariable("number") int number,
            @Valid @RequestBody WorkRequest request) { return selections.administrativeValues(request.workId(), id, number); }
    @GetMapping("/document-registrations/{id}")
    public RegisterTemplateVersionService.OperationView status(@PathVariable("id") UUID id) { return versions.status(id); }
    @PutMapping("/document-templates/{id}/versions/{number}/name")
    public TemplateManagementService.VersionView renameVersion(@PathVariable("id") Long id, @PathVariable("number") int number,
            @Valid @RequestBody VersionName request) { return templates.renameVersion(id, number, request.name()); }
    @DeleteMapping("/document-templates/{id}/versions/{number}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVersion(@PathVariable("id") Long id, @PathVariable("number") int number) { templates.deleteVersion(id, number); }
    public record VersionName(@NotBlank @Size(max = 150) String name) { }
    @PostMapping("/document-registrations/{id}/recover")
    public RegisterTemplateVersionService.OperationView recover(@PathVariable("id") UUID id) { return versions.recover(id); }

    public record TemplateRequest(@NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 100) String documentType, @NotNull TemplateState state,
            @Positive Long clientId, @Positive Long workId) { }
    public record ConfirmationRequest(@NotNull UUID confirmationId, @NotBlank String fingerprint,
            @NotNull @Size(max = 5000) Map<@Positive Long, @NotNull Boolean> required, boolean staticAccepted,
            @NotBlank @Size(max = 150) String name) { }
    public record WorkRequest(@NotNull @Positive Long workId) { }
}
