package com.brica.works.application.port;

import java.util.List;
import java.util.Optional;

import com.brica.works.domain.Work;

public interface WorkStore {

    Work save(Work work);

    Optional<Work> findById(Long id);

    List<Work> list();
}
