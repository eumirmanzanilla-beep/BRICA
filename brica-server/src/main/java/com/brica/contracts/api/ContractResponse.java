package com.brica.contracts.api;

import java.util.Map;

import com.brica.contracts.domain.Contract;

public record ContractResponse(
        Long id,
        Long workId,
        Map<String, Object> data
) {

    public static ContractResponse from(Contract contract) {
        return new ContractResponse(
            contract.getId(),
            contract.getWork().getId(),
            contract.getData()
        );
    }
}