package com.brica.clientes.api;

import com.brica.clientes.application.RegisterClientService;
import com.brica.clientes.domain.Cliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterClientService registerClientService;

    @Test
    void shouldRegisterClient() throws Exception {
        Cliente client = new Cliente(
            "Constructora ABC",
            "Jane Doe"
        );

        when(registerClientService.registerClient(
            "Constructora ABC",
            "Jane Doe"
        )).thenReturn(client);

        mockMvc.perform(
                post("/api/v1/clients")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "name": "Constructora ABC",
                          "legalRepresentative": "Jane Doe"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.name")
                    .value("Constructora ABC")
            )
            .andExpect(
                jsonPath("$.legalRepresentative")
                    .value("Jane Doe")
            );

        verify(registerClientService).registerClient(
            "Constructora ABC",
            "Jane Doe"
        );
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        mockMvc.perform(
                post("/api/v1/clients")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "name": "",
                          "legalRepresentative": "Jane Doe"
                        }
                        """)
            )
            .andExpect(status().isBadRequest());

        verifyNoInteractions(registerClientService);
    }
}