package com.brica.contracts.application;

import java.util.Map;

import com.brica.contracts.application.port.ContractStore;
import com.brica.contracts.domain.Contract;
import com.brica.works.application.WorkNotFoundException;
import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterContractService {

    private final WorkStore workStore;
    private final ContractStore contractStore;

    public RegisterContractService(
            WorkStore workStore,
            ContractStore contractStore) {

        this.workStore = workStore;
        this.contractStore = contractStore;
    }

    @Transactional
    public Contract registerContract(
            Long workId,
            Map<String, Object> data) {

        Work work = workStore.findById(workId)
                .orElseThrow(() ->
                    new WorkNotFoundException(workId)
                );

        Contract contract = new Contract(
            work,
            data
        );

        return contractStore.save(contract);
    }
}
