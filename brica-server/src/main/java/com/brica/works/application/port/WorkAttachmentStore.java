package com.brica.works.application.port;

import com.brica.works.domain.WorkAttachment;
import java.util.List;
import java.util.Optional;

public interface WorkAttachmentStore {
    WorkAttachment save(WorkAttachment value);
    List<WorkAttachment> list(Long workId, int page, int size);
    Optional<WorkAttachment> find(Long id, Long workId);
    Optional<WorkAttachment> evidence(Long id, Long workId);
}
