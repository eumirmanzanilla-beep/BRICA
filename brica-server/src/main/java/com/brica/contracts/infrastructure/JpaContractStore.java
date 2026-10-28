package com.brica.contracts.infrastructure;

import com.brica.contracts.application.port.ContractStore;
import com.brica.contracts.domain.Contract;

import org.springframework.stereotype.Repository;

@Repository
public class JpaContractStore implements ContractStore {

    private final ContractRepository contractRepository;

    public JpaContractStore(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Override
    public Contract save(Contract contract) {
        return contractRepository.save(contract);
    }
}
