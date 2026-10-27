package com.brica.contracts.api;

import java.util.Map;

import com.brica.contracts.application.RegisterContractService;
import com.brica.contracts.domain.Contract;
import com.brica.server.shared.error.api.ApiExceptionHandler;
import com.brica.works.application.WorkNotFoundException;
import com.brica.works.domain.Work;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractController.class)
@Import(ApiExceptionHandler.class)
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterContractService registerContractService;

    @Test
    void shouldRegisterContract() throws Exception {
        Work work = mock(Work.class);
        Contract contract = mock(Contract.class);

        Map<String, Object> data = Map.of(
            "contractNumber", "ABC-2026-001",
            "amount", 1500000
        );

        when(work.getId())
            .thenReturn(1L);

        when(contract.getId())
            .thenReturn(10L);

        when(contract.getWork())
            .thenReturn(work);

        when(contract.getData())
            .thenReturn(data);

        when(registerContractService.registerContract(
            1L,
            data
        )).thenReturn(contract);

        mockMvc.perform(
                post("/api/v1/contracts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "workId": 1,
                          "data": {
                            "contractNumber": "ABC-2026-001",
                            "amount": 1500000
                          }
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.id").value(10)
            )
            .andExpect(
                jsonPath("$.workId").value(1)
            )
            .andExpect(
                jsonPath("$.data.contractNumber")
                    .value("ABC-2026-001")
            )
            .andExpect(
                jsonPath("$.data.amount")
                    .value(1500000)
            );

        verify(registerContractService)
            .registerContract(1L, data);
    }

    @Test
    void shouldRejectContractWhenWorkDoesNotExist()
            throws Exception {

        when(registerContractService.registerContract(
            999L,
            Map.of()
        )).thenThrow(
            new WorkNotFoundException(999L)
        );

        mockMvc.perform(
                post("/api/v1/contracts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "workId": 999,
                          "data": {}
                        }
                        """)
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.title")
                    .value("Work not found")
            )
            .andExpect(
                jsonPath("$.detail")
                    .value("Work not found with id: 999")
            );
    }

    @Test
    void shouldRejectMissingWorkId() throws Exception {

        mockMvc.perform(
                post("/api/v1/contracts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "data": {
                            "contractNumber": "ABC-001"
                          }
                        }
                        """)
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(registerContractService);
    }
}