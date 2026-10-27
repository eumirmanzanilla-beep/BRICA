package com.brica.works.infrastructure;

import java.util.List;
import java.util.Optional;

import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;

import org.springframework.stereotype.Repository;

@Repository
public class JpaWorkStore implements WorkStore {

    private final WorkRepository workRepository;

    public JpaWorkStore(WorkRepository workRepository) {
        this.workRepository = workRepository;
    }

    @Override
    public Work save(Work work) {
        return workRepository.save(work);
    }

    @Override
    public Optional<Work> findById(Long id) {
        return workRepository.findById(id);
    }

    @Override
    public List<Work> list() {
        return workRepository.findAllByOrderByIdAsc();
    }
}
