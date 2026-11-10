package com.brica.documents.api;

import java.util.List;

import com.brica.documents.application.ListReferenceFieldsService;
import com.brica.documents.application.RegisterReferenceFieldService;
import com.brica.documents.application.UpdateReferenceFieldService;
import com.brica.documents.domain.ReferenceAttribute;
import com.brica.documents.domain.ReferenceDataType;
import com.brica.documents.domain.ReferenceField;
import com.brica.documents.domain.ReferenceOrigin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ReferenceFieldController {
    private final ListReferenceFieldsService list;
    private final RegisterReferenceFieldService register;
    private final UpdateReferenceFieldService update;

    public ReferenceFieldController(ListReferenceFieldsService list,
            RegisterReferenceFieldService register, UpdateReferenceFieldService update) {
        this.list = list;
        this.register = register;
        this.update = update;
    }

    @GetMapping("/document-attributes")
    public List<AttributeResponse> attributes() {
        return list.listApprovedAttributes().stream().map(a -> new AttributeResponse(a,
                a.getLabel(), a.getType(), a.getOrigin(), a.getSourcePath())).toList();
    }

    @GetMapping("/reference-fields")
    public List<FieldResponse> fields(@RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        return list.list(page, size).stream().map(FieldResponse::from).toList();
    }

    @PostMapping("/reference-fields")
    @ResponseStatus(HttpStatus.CREATED)
    public FieldResponse register(@Valid @RequestBody RegisterFieldRequest request) {
        return FieldResponse.from(register.register(request.key(), request.visibleName(), request.attribute()));
    }

    @PutMapping("/reference-fields/{id}")
    public FieldResponse rename(@PathVariable("id") Long id, @Valid @RequestBody RenameFieldRequest request) {
        return FieldResponse.from(update.rename(id, request.visibleName()));
    }

    public record RegisterFieldRequest(@NotBlank @Size(max = 50) String key,
            @NotBlank @Size(max = 50) String visibleName, @NotNull ReferenceAttribute attribute) { }
    public record RenameFieldRequest(@NotBlank @Size(max = 50) String visibleName) { }
    public record AttributeResponse(ReferenceAttribute id, String label, ReferenceDataType type,
            ReferenceOrigin origin, String sourcePath) { }
    public record FieldResponse(Long id, String key, String visibleName, ReferenceDataType type,
            ReferenceOrigin origin, String sourcePath) {
        public static FieldResponse from(ReferenceField field) {
            return new FieldResponse(field.getId(), field.getKey(), field.getVisibleName(),
                    field.getType(), field.getOrigin(), field.getSourcePath());
        }
    }
}
