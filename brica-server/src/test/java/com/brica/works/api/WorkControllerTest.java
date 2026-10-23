package com.brica.works.api;

import java.time.LocalDate;

import com.brica.clientes.application.ClientNotFoundException;
import com.brica.clientes.domain.Cliente;
import com.brica.server.shared.error.api.ApiExceptionHandler;
import com.brica.works.application.RegisterWorkService;
import com.brica.works.domain.Work;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkController.class)
@Import(ApiExceptionHandler.class)
class WorkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterWorkService registerWorkService;

    @Test
    void shouldRegisterWork() throws Exception {

        Cliente client = mock(Cliente.class);

        when(client.getIdCliente())
            .thenReturn(1L);

        Work work = mock(Work.class);

        when(work.getId())
            .thenReturn(10L);

        when(work.getClient())
            .thenReturn(client);

        when(work.getName())
            .thenReturn("Administrative Building Renovation");

        when(work.getLocation())
            .thenReturn("Merida, Yucatan");

        when(work.getDescription())
            .thenReturn("General renovation work");

        when(work.getStartDate())
            .thenReturn(LocalDate.of(2026, 10, 23));

        when(work.getEndDate())
            .thenReturn(LocalDate.of(2027, 1, 30));

        when(registerWorkService.registerWork(
            1L,
            "Administrative Building Renovation",
            "Merida, Yucatan",
            "General renovation work",
            LocalDate.of(2026, 10, 23),
            LocalDate.of(2027, 1, 30)
        )).thenReturn(work);

        mockMvc.perform(
                post("/api/v1/works")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                        "clientId": 1,
                        "name": "Administrative Building Renovation",
                        "location": "Merida, Yucatan",
                        "description": "General renovation work",
                        "startDate": "2026-10-23",
                        "endDate": "2027-01-30"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.id").value(10)
            )
            .andExpect(
                jsonPath("$.clientId").value(1)
            )
            .andExpect(
                jsonPath("$.name")
                    .value("Administrative Building Renovation")
            )
            .andExpect(
                jsonPath("$.location")
                    .value("Merida, Yucatan")
            );
    }

    @Test
    void shouldRejectWorkWhenClientDoesNotExist()
            throws Exception {

        when(registerWorkService.registerWork(
            999L,
            "Invalid Work",
            "Merida, Yucatan",
            null,
            LocalDate.of(2026, 10, 23),
            null
        )).thenThrow(new ClientNotFoundException(999L));

        mockMvc.perform(
                post("/api/v1/works")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "clientId": 999,
                          "name": "Invalid Work",
                          "location": "Merida, Yucatan",
                          "startDate": "2026-10-23"
                        }
                        """)
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.title")
                    .value("Client not found")
            );
    }

    @Test
    void shouldRejectBlankWorkName() throws Exception {
        mockMvc.perform(
                post("/api/v1/works")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "clientId": 1,
                          "name": "",
                          "location": "Merida, Yucatan",
                          "startDate": "2026-10-23"
                        }
                        """)
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(registerWorkService);
    }
}