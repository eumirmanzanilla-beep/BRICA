
package com.brica.works.infrastructure;

import java.util.List;
import java.util.Optional;

import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaWorkStore implements WorkStore {

    private final WorkRepository repository;

    public JpaWorkStore(WorkRepository repository) {
        this.repository = repository;
    }

    @Override
    public Work save(Work work) {
        return repository.save(work);
    }

    @Override
    public Optional<Work> findById(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<Work> list(int page, int size) {
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by("id")
        );

        return repository
                .findAllByDeletedAtIsNull(pageable)
                .getContent();
    }

    @Override
    public Optional<Work> findForUpdate(Long id) {
        return repository.findForUpdate(id);
    }
}
