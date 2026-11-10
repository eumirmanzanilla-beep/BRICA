package com.brica.works.application;
import org.springframework.security.access.prepost.PreAuthorize;

import com.brica.works.domain.Work;
import com.brica.works.application.port.WorkStore;
import com.brica.server.shared.validation.domain.InputRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("@accessPolicy.hasPermission('WORKS_WRITE')")
public class CorrectWorkService {
    private final WorkStore repository;

    public CorrectWorkService(WorkStore repository) {
        this.repository = repository;
    }

    @Transactional
    public Work correct(Long id, String expectedEtag, String name, String location, String description) {
        InputRules.positiveId("id", id);
        if (expectedEtag == null || expectedEtag.isBlank()) {
            throw new MissingPreconditionException();
        }
        var work = repository.findForUpdate(id).orElseThrow(() -> new WorkNotFoundException(id));
        if (!WorkRevision.etag(work).equals(expectedEtag)) {
            throw new StaleWorkException();
        }
        work.correctGeneralInformation(name, location, description);
        return repository.save(work);
    }
}
