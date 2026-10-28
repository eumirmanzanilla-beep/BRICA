package com.brica.contracts.application.port;

import com.brica.contracts.domain.Contract;

public interface ContractStore {
    Contract save(Contract entity);
}
