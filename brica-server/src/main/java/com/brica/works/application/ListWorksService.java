package com.brica.works.application;

import java.util.List;

import com.brica.works.domain.Work;
import com.brica.works.application.port.WorkStore;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListWorksService {

    private final WorkStore workRepository;

    public ListWorksService(WorkStore workRepository) {
        this.workRepository = workRepository;
    }

    @Transactional(readOnly = true)
    public List<Work> listWorks() {
        return listWorks(0, 50);
    }
    @Transactional(readOnly = true)
    public List<Work> listWorks(int page, int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new com.brica.server.shared.validation.domain.InvalidDataException(
                    java.util.Map.of("pagination", "Usa page >= 0 y size entre 1 y 200."));
        }
        return workRepository.list(page, size);
    }
}
