package com.brica.contracts.api;

import com.brica.contracts.application.RegisterContractService;
import com.brica.contracts.domain.Contract;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final RegisterContractService registerContractService;

    public ContractController(
            RegisterContractService registerContractService) {

        this.registerContractService = registerContractService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContractResponse registerContract(
            @Valid @RequestBody RegisterContractRequest request) {

        Contract contract =
            registerContractService.registerContract(
                request.workId(),
                request.data()
            );

        return ContractResponse.from(contract);
    }
}