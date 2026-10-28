
package com.brica.works.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.brica.server.shared.validation.domain.InputRules;
import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteWorkService {

    private final WorkStore workStore;

    public DeleteWorkService(WorkStore workStore) {
        this.workStore = workStore;
    }

    @Transactional
    public void delete(Long id) {

        InputRules.positiveId("id", id);

        Work work = workStore.findForUpdate(id)
                .orElseThrow(
                        () -> new WorkNotFoundException(id)
                );

        work.markDeleted(
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        workStore.save(work);
    }
}
