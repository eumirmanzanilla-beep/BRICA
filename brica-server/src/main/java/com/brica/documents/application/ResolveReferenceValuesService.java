package com.brica.documents.application;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.application.port.WorkReferenceSource;
import com.brica.documents.domain.ReferenceField;
import com.brica.documents.domain.ReferenceValue;
import com.brica.server.shared.validation.domain.InputRules;
import com.brica.server.shared.validation.domain.InvalidDataException;
import com.brica.works.application.WorkNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@accessPolicy.hasPermission('DOCUMENT_VALUES_READ')")
public class ResolveReferenceValuesService {
    private final ReferenceFieldStore fields;
    private final WorkReferenceSource source;

    public ResolveReferenceValuesService(ReferenceFieldStore fields, WorkReferenceSource source) {
        this.fields = fields;
        this.source = source;
    }

    @Transactional(readOnly = true)
    public List<ReferenceValue> resolveForWork(Long workId, List<Long> fieldIds) {
        InputRules.positiveId("workId", workId);
        if (fieldIds == null) {
            throw new InvalidDataException(Map.of("fieldIds", "Indica los campos que deseas consultar."));
        }
        fieldIds.forEach(id -> InputRules.positiveId("fieldIds", id));
        var ids = new LinkedHashSet<>(fieldIds);
        var data = source.findByWorkId(workId).orElseThrow(() -> new WorkNotFoundException(workId));
        Map<Long, ReferenceField> found = fields.findAllById(ids).stream()
                .collect(Collectors.toMap(ReferenceField::getId, Function.identity()));
        return ids.stream().map(id -> {
            var field = found.get(id);
            if (field == null) {
                throw new ReferenceFieldNotFoundException(id);
            }
            return ReferenceValue.resolve(field, data.valueOf(field));
        }).toList();
    }
}
