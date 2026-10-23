package com.brica.contracts.application;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brica.contracts.domain.Contract;
import com.brica.contracts.infrastructure.ContractRepository;
import com.brica.works.application.WorkNotFoundException;
import com.brica.works.domain.Work;
import com.brica.works.infrastructure.WorkRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterContractServiceTest {

    @Mock
    private WorkRepository workRepository;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private RegisterContractService registerContractService;

    @Test
    void shouldRegisterContractForExistingWork() {
        Work work = mock(Work.class);

        Map<String, Object> data = Map.of(
            "contractNumber", "ABC-2026-001",
            "amount", 1500000
        );

        when(workRepository.findById(1L))
            .thenReturn(Optional.of(work));

        when(contractRepository.save(any(Contract.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Contract contract =
            registerContractService.registerContract(
                1L,
                data
            );

        assertEquals(work, contract.getWork());
        assertEquals(
            "ABC-2026-001",
            contract.getData().get("contractNumber")
        );
        assertEquals(
            1500000,
            contract.getData().get("amount")
        );

        verify(workRepository).findById(1L);
        verify(contractRepository)
            .save(any(Contract.class));
    }

    @Test
    void shouldRejectContractWhenWorkDoesNotExist() {
        when(workRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThrows(
            WorkNotFoundException.class,
            () -> registerContractService.registerContract(
                999L,
                Map.of(
                    "contractNumber",
                    "INVALID"
                )
            )
        );

        verify(workRepository).findById(999L);

        verify(contractRepository, never())
            .save(any(Contract.class));
    }
}