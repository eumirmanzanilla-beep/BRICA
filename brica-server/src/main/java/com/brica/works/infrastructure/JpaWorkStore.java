package com.brica.works.infrastructure;

import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;
import org.springframework.stereotype.Repository;

@Repository
public class JpaWorkStore implements WorkStore {
    private final WorkRepository repository;

    public JpaWorkStore(WorkRepository repository) { this.repository = repository; }

    @Override
    public Work save(Work entity) { return repository.save(entity); }
    @Override
    public java.util.Optional<Work> findById(Long id) { return repository.findById(id); }

    @Override
    public java.util.List<Work> list(int page, int size) {
        return repository.findAll(org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("id"))).getContent();
    }
    @Override
    public java.util.Optional<Work> findForUpdate(Long id) { return repository.findForUpdate(id); }
}
