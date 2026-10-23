package com.brica.contracts.application;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.brica.contracts.domain.Contract;
import com.brica.contracts.infrastructure.ContractRepository;
import com.brica.works.application.WorkNotFoundException;
import com.brica.works.domain.Work;
import com.brica.works.infrastructure.WorkRepository;

@Service
public class RegisterContractService {

    private final WorkRepository workRepository;
    private final ContractRepository contractRepository;

    public RegisterContractService(
            WorkRepository workRepository,
            ContractRepository contractRepository) {

        this.workRepository = workRepository;
        this.contractRepository = contractRepository;
    }

    @Transactional
    public Contract registerContract(
            Long workId,
            Map<String, Object> data) {

        Work work = workRepository.findById(workId)
                .orElseThrow(() ->
                    new WorkNotFoundException(workId)
                );

        Contract contract = new Contract(
            work,
            data
        );

        return contractRepository.save(contract);
    }
}