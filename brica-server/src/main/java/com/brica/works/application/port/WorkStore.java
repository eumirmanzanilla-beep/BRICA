package com.brica.works.application.port;

import com.brica.works.domain.Work;

public interface WorkStore {
    Work save(Work entity);
    java.util.Optional<Work> findById(Long id);
    java.util.List<Work> list(int page, int size);
    java.util.Optional<Work> findForUpdate(Long id);
}
