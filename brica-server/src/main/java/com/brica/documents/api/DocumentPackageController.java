package com.brica.documents.api;

import java.util.List;
import com.brica.documents.application.DocumentPackageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/document-packages")
public class DocumentPackageController {
    private final DocumentPackageService service;
    public DocumentPackageController(DocumentPackageService service) { this.service = service; }
    @GetMapping
    public List<DocumentPackageService.PackageView> list(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @RequestParam(name = "clientId", required = false) Long clientId,
            @RequestParam(name = "workId", required = false) Long workId) {
        return workId != null ? service.listForWork(workId, page, size)
                : clientId != null ? service.listForClient(clientId, page, size) : service.list(page, size);
    }
    @GetMapping("/{id}")
    public ResponseEntity<DocumentPackageService.PackageView> get(@PathVariable("id") Long id) { return response(service.get(id)); }
    @PostMapping
    public ResponseEntity<DocumentPackageService.PackageView> create(@Valid @RequestBody PackageRequest request) {
        var value = request.workId() == null
                ? service.create(request.clientId(), request.name(), request.templateIds(), request.confirm())
                : service.createFromWork(request.workId(), request.name(), request.templateIds(), request.confirm());
        return ResponseEntity.status(HttpStatus.CREATED).eTag(DocumentPackageService.etag(value.revision())).body(value);
    }
    @PutMapping("/{id}")
    public ResponseEntity<DocumentPackageService.PackageView> update(@PathVariable("id") Long id,
            @RequestHeader(value = "If-Match", required = false) String expected,
            @Valid @RequestBody PackageRequest request) {
        return response(request.workId() == null
                ? service.update(id, expected, request.clientId(), request.name(), request.templateIds(), request.confirm())
                : service.updateFromWork(id, expected, request.workId(), request.name(), request.templateIds(), request.confirm()));
    }
    @DeleteMapping("/{id}/draft") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discard(@PathVariable("id") Long id, @RequestHeader(value = "If-Match", required = false) String expected) {
        service.discard(id, expected);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id, @RequestHeader(value = "If-Match", required = false) String expected) {
        service.discard(id, expected);
    }
    @PostMapping("/{id}/confirmation")
    public ResponseEntity<DocumentPackageService.PackageView> confirm(@PathVariable("id") Long id,
            @RequestHeader(value = "If-Match", required = false) String expected) {
        return response(service.confirmReady(id, expected));
    }
    @PostMapping("/{id}/templates")
    public ResponseEntity<DocumentPackageService.CreatedTemplate> createTemplate(@PathVariable("id") Long id,
            @RequestHeader(value = "If-Match", required = false) String expected,
            @Valid @RequestBody NewTemplate request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTemplate(id, expected, request.name(), request.documentType()));
    }
    public record NewTemplate(@NotBlank @Size(max = 150) String name, @NotBlank @Size(max = 100) String documentType) { }
    private ResponseEntity<DocumentPackageService.PackageView> response(DocumentPackageService.PackageView value) {
        return ResponseEntity.ok().eTag(DocumentPackageService.etag(value.revision())).body(value);
    }
    public record PackageRequest(@Positive Long clientId, @Size(max = 150) String name,
            @NotNull @Size(max = 100) List<@NotNull @Positive Long> templateIds, boolean confirm, @Positive Long workId) {
        @AssertTrue(message = "Selecciona un cliente o una obra, no ambos.")
        public boolean isSingleContext() { return clientId == null || workId == null; }
    }
}
