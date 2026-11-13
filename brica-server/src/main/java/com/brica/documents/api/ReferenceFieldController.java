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
        ReferenceAttribute.forKey(request.key()).ifPresent(internal -> {
            if (request.attribute() != null && request.attribute() != internal) {
                throw new com.brica.server.shared.validation.domain.InvalidDataException(java.util.Map.of("key", "Esta clave tiene un origen interno predefinido."));
            }
        });
        if ((request.attribute() == null) == (request.additionalType() == null)) {
            throw new com.brica.server.shared.validation.domain.InvalidDataException(java.util.Map.of(
                    "attribute", "Selecciona un atributo existente o un tipo de campo adicional."));
        }
        return FieldResponse.from(request.attribute() != null
                ? register.register(request.key(), request.key(), request.attribute())
                : register.registerInternal(request.key(), request.additionalType()));
    }

    @PutMapping("/reference-fields/{id}")
    public FieldResponse rename(@PathVariable("id") Long id, @Valid @RequestBody RenameFieldRequest request) {
        return FieldResponse.from(update.alignName(id));
    }

    @DeleteMapping("/reference-fields/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) { update.delete(id); }

    public record RegisterFieldRequest(@NotBlank @Size(max = 50) String key,
            @Size(max = 50) String visibleName, ReferenceAttribute attribute,
            ReferenceDataType additionalType) { }
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
