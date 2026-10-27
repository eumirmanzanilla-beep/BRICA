package com.brica.works.application;

import java.util.List;

import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListWorksService {

    private final WorkStore workStore;

    public ListWorksService(WorkStore workStore) {
        this.workStore = workStore;
    }

    @Transactional(readOnly = true)
    public List<Work> listWorks() {
        return workStore.list();
    }
}
