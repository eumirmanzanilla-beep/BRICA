package com.brica.documents.api;

import java.util.List;

import com.brica.documents.application.ResolveReferenceValuesService;
import com.brica.documents.domain.ReferenceValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReferenceValueController {
    private final ResolveReferenceValuesService service;
    public ReferenceValueController(ResolveReferenceValuesService service) { this.service = service; }

    @PostMapping("/api/v1/reference-values/resolve")
    public List<ReferenceValue> resolve(@Valid @RequestBody ResolveRequest request) {
        return service.resolveForWork(request.workId(), request.fieldIds());
    }

    public record ResolveRequest(@NotNull @Positive Long workId,
            @NotNull @Size(max = 200) List<@NotNull @Positive Long> fieldIds) { }
}
