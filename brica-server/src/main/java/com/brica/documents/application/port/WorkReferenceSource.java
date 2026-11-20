package com.brica.documents.application.port;

import java.util.Optional;

import com.brica.documents.application.WorkReferenceData;

public interface WorkReferenceSource {
    Optional<WorkReferenceData> findByWorkId(Long workId);
}
