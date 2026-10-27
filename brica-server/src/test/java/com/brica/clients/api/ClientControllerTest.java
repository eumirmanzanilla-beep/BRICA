package com.brica.clients.api;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.brica.clients.api.ClientController;
import com.brica.clients.application.ListClientsService;
import com.brica.clients.application.RegisterClientService;
import com.brica.clients.domain.Client;

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
        Client firstClient = mock(Client.class);
        Client secondClient = mock(Client.class);

        when(firstClient.getId())
            .thenReturn(1L);

        when(firstClient.getName())
            .thenReturn("Constructora ABC");

        when(firstClient.getLegalRepresentative())
            .thenReturn("Jane Doe");

        when(secondClient.getId())
            .thenReturn(2L);

        when(secondClient.getName())
            .thenReturn("Constructora XYZ");

        when(secondClient.getLegalRepresentative())
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
        Client client = new Client(
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