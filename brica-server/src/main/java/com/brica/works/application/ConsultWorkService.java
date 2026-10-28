package com.brica.works.application;

import com.brica.works.domain.Work;
import com.brica.works.application.port.WorkStore;
import com.brica.server.shared.validation.domain.InputRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
