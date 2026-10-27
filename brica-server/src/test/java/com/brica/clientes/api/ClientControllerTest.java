package com.brica.clientes.api;

import java.util.List;

import com.brica.clientes.application.ListClientsService;
import com.brica.clientes.application.RegisterClientService;
import com.brica.clientes.domain.Cliente;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterClientService registerClientService;

    @MockitoBean
    private ListClientsService listClientsService;

    @Test
    void shouldListClients() throws Exception {
        Cliente firstClient = mock(Cliente.class);
        Cliente secondClient = mock(Cliente.class);

        when(firstClient.getIdCliente())
            .thenReturn(1L);

        when(firstClient.getNombre())
            .thenReturn("Constructora ABC");

        when(firstClient.getRepresentanteLegal())
            .thenReturn("Jane Doe");

        when(secondClient.getIdCliente())
            .thenReturn(2L);

        when(secondClient.getNombre())
            .thenReturn("Constructora XYZ");

        when(secondClient.getRepresentanteLegal())
            .thenReturn("John Doe");

        when(listClientsService.listClients())
            .thenReturn(List.of(firstClient, secondClient));

        mockMvc.perform(
                get("/api/v1/clients")
            )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$[0].id")
                    .value(1)
            )
            .andExpect(
                jsonPath("$[0].name")
                    .value("Constructora ABC")
            )
            .andExpect(
                jsonPath("$[0].legalRepresentative")
                    .value("Jane Doe")
            )
            .andExpect(
                jsonPath("$[1].id")
                    .value(2)
            )
            .andExpect(
                jsonPath("$[1].name")
                    .value("Constructora XYZ")
            );

        verify(listClientsService).listClients();
    }

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