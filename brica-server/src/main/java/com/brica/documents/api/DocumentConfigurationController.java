package com.brica.documents.api;

import java.util.List;
import com.brica.documents.application.DocumentSelectionService;
import com.brica.documents.application.DocumentSelectionService.Selection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/works/{id}")
public class DocumentConfigurationController {
    private final DocumentSelectionService service;
    public DocumentConfigurationController(DocumentSelectionService service) { this.service = service; }
    @GetMapping("/document-options")
    public List<DocumentSelectionService.PackageOption> options(@PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "size", defaultValue = "50") int size) {
        return service.options(id, page, size);
    }
    @PostMapping("/document-selection/resolve")
    public List<DocumentSelectionService.ResolvedDocument> resolve(@PathVariable("id") Long id, @Valid @RequestBody SelectionRequest request) {
        return service.resolve(id, request.packageId(), request.packageRevision(), request.selections());
    }
    public record SelectionRequest(@NotNull @Positive Long packageId, @NotNull @PositiveOrZero Long packageRevision,
            @NotEmpty @Size(max = 100) List<@NotNull @Valid Selection> selections) { }
}
