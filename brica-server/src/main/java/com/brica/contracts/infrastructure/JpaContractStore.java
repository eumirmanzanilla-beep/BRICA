package com.brica.contracts.infrastructure;

import com.brica.contracts.application.port.ContractStore;
import com.brica.contracts.domain.Contract;
import org.springframework.stereotype.Repository;

@Repository
public class JpaContractStore implements ContractStore {
    private final ContractRepository repository;

    public JpaContractStore(ContractRepository repository) { this.repository = repository; }

    @Override
    public Contract save(Contract entity) { return repository.save(entity); }
}
