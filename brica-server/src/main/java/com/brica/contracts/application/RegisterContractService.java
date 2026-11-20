package com.brica.contracts.application;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.brica.contracts.domain.Contract;
import com.brica.contracts.application.port.ContractStore;
import com.brica.works.application.WorkNotFoundException;
import com.brica.works.domain.Work;
import com.brica.works.application.port.WorkStore;

@Service
@PreAuthorize("@accessPolicy.hasPermission('CONTRACTS_WRITE')")
public class RegisterContractService {

    private final WorkStore workRepository;
    private final ContractStore contractRepository;

    public RegisterContractService(
            WorkStore workRepository,
            ContractStore contractRepository) {

        this.workRepository = workRepository;
        this.contractRepository = contractRepository;
    }

    @Transactional
    public Contract registerContract(
            Long workId,
            Map<String, Object> data) {

        com.brica.server.shared.validation.domain.InputRules.positiveId("workId", workId);
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
