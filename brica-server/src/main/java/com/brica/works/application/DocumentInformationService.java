package com.brica.works.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.domain.ReferenceField;
import com.brica.documents.domain.ReferenceOrigin;
import com.brica.documents.domain.ReferenceValue;
import com.brica.documents.domain.ResolutionStatus;
import com.brica.server.shared.validation.domain.InputRules;
import com.brica.server.shared.validation.domain.InvalidDataException;
import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;
import com.brica.works.domain.WorkStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@accessPolicy.hasPermission('WORKS_WRITE')")
public class DocumentInformationService {
    private final WorkStore works;
    private final ReferenceFieldStore fields;
    public DocumentInformationService(WorkStore works, ReferenceFieldStore fields) { this.works = works; this.fields = fields; }

    @Transactional
    public Work update(Long id, String expectedEtag, Information information) {
        InputRules.positiveId("id", id);
        if (expectedEtag == null || expectedEtag.isBlank()) { throw new MissingPreconditionException(); }
        var work = works.findForUpdate(id).orElseThrow(() -> new WorkNotFoundException(id));
        if (!WorkRevision.etag(work).equals(expectedEtag)) { throw new StaleWorkException(); }
        if (information == null || information.additionalValues() == null) {
            throw new InvalidDataException(Map.of("additionalValues", "Indica los valores adicionales."));
        }
        var entries = information.additionalValues();
        var keys = new java.util.HashSet<>(work.getAdditionalValues().keySet());
        keys.addAll(entries.keySet());
        var definitions = fields.findAllByKey(keys).stream()
                .collect(Collectors.toMap(ReferenceField::getKey, f -> f));
        var errors = new LinkedHashMap<String, String>();
        entries.forEach((key, value) -> {
            var definition = definitions.get(key);
            if (definition == null && java.util.Objects.equals(work.getAdditionalValues().get(key), value)
                    && work.getAdditionalValues().containsKey(key)) {
                // A deleted catalog definition must not make its historical value uneditable by accident.
            } else if (definition == null || definition.getOrigin() != ReferenceOrigin.ADDITIONAL) {
                errors.put(key, "Selecciona una clave adicional registrada.");
            } else if (ReferenceValue.resolve(definition, value).status() == ResolutionStatus.INVALID) {
                errors.put(key, "El valor no corresponde al tipo del campo.");
            }
        });
        InputRules.rejectErrors(errors);
        var nonNull = new LinkedHashMap<String, String>();
        entries.forEach((k, v) -> { if (v != null && !v.isBlank()) { nonNull.put(k, v); } });
        work.getAdditionalValues().forEach((key, value) -> {
            if (!definitions.containsKey(key)) { nonNull.put(key, value); }
        });
        work.updateDocumentInformation(information.contractNumber(), information.contractor(), information.contractedAmount(),
                information.amountBeforeTax(), information.taxAmount(), information.startDate(), information.endDate(),
                information.actualEndDate(), information.status(), nonNull);
        return works.save(work);
    }
    public record Information(String contractNumber, String contractor, BigDecimal contractedAmount,
            BigDecimal amountBeforeTax, BigDecimal taxAmount, LocalDate startDate, LocalDate endDate,
            LocalDate actualEndDate, WorkStatus status,
            Map<String, String> additionalValues) { }
    @Transactional
    public Work updateAll(Long id, String expectedEtag, CompleteInformation information) {
        if (information == null) { throw new InvalidDataException(Map.of("information", "Indica la información de la obra.")); }
        var work = update(id, expectedEtag, information.documentInformation());
        work.correctGeneralInformation(information.name(), information.location(), information.description());
        return works.save(work);
    }
    public record CompleteInformation(String name, String location, String description, Information documentInformation) { }
}
