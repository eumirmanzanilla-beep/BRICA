package com.brica.works.application;
import org.springframework.security.access.prepost.PreAuthorize;

import com.brica.works.domain.Work;
import com.brica.works.application.port.WorkStore;
import com.brica.server.shared.validation.domain.InputRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@accessPolicy.hasPermission('WORKS_READ')")
public class ConsultWorkService {
    private final WorkStore repository;

    public ConsultWorkService(WorkStore repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Work get(Long id) {
        InputRules.positiveId("id", id);
        return repository.findById(id).orElseThrow(() -> new WorkNotFoundException(id));
    }
}
