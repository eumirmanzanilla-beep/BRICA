package com.brica.works.application;

import java.util.List;

import com.brica.works.domain.Work;
import com.brica.works.infrastructure.WorkRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListWorksService {

    private final WorkRepository workRepository;

    public ListWorksService(WorkRepository workRepository) {
        this.workRepository = workRepository;
    }

    @Transactional(readOnly = true)
    public List<Work> listWorks() {
        return workRepository.findAllByOrderByIdAsc();
    }
}