package com.brica.contracts.infrasctructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.brica.contracts.domain.Contract;

public interface ContractRepository
        extends JpaRepository<Contract, Long> {
}