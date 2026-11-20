
package com.brica.clients.api;

import java.util.List;

import com.brica.clients.application.ConsultClientService;
import com.brica.clients.application.DeleteClientService;
import com.brica.clients.application.ListClientsService;
import com.brica.clients.application.RegisterClientService;
import com.brica.clients.application.UpdateClientService;
import com.brica.clients.domain.Client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@org.springframework.context.annotation.Import({
        com.brica.server.security.infrastructure.SecurityConfiguration.class,
        com.brica.server.security.infrastructure.PasswordConfiguration.class})
@org.springframework.security.test.context.support.WithMockUser
class ClientControllerTest {
    @MockitoBean private com.brica.server.security.infrastructure.AppUserRepository securityUsers;
    @Autowired private org.springframework.web.context.WebApplicationContext context;

    @org.junit.jupiter.api.BeforeEach
    void prepareAuthenticatedRequests() {
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(get("/").secure(true).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).build();
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterClientService registerClientService;

    @MockitoBean
    private ListClientsService listClientsService;

    @MockitoBean
    private ConsultClientService consultClientService;

    @MockitoBean
    private UpdateClientService updateClientService;

    @MockitoBean
    private DeleteClientService deleteClientService;

    @Test
    void shouldListClients() throws Exception {

        Client first = mock(Client.class);
        Client second = mock(Client.class);

        when(first.getId()).thenReturn(1L);
        when(first.getName()).thenReturn("Constructora ABC");
        when(first.getLegalRepresentative()).thenReturn("Jane Doe");

        when(second.getId()).thenReturn(2L);
        when(second.getName()).thenReturn("Constructora XYZ");
        when(second.getLegalRepresentative()).thenReturn("John Doe");

        when(listClientsService.listClients())
                .thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Constructora ABC"))
                .andExpect(jsonPath("$[0].legalRepresentative").value("Jane Doe"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Constructora XYZ"));

        verify(listClientsService).listClients();
    }

    @Test
    void shouldRegisterClient() throws Exception {

        Client client = new Client(
                "Constructora_ABC",
                "Jane Doe"
        );

        when(registerClientService.registerClient(
                "Constructora_ABC",
                "Jane Doe"
        )).thenReturn(client);

        mockMvc.perform(
                post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Constructora_ABC",
                                  "legalRepresentative": "Jane Doe"
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Constructora_ABC"))
                .andExpect(jsonPath("$.legalRepresentative").value("Jane Doe"));

        verify(registerClientService)
                .registerClient("Constructora_ABC", "Jane Doe");
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
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(registerClientService);
    }

    @Test
    void shouldConsultClient() throws Exception {

        Client client = mock(Client.class);

        when(client.getId()).thenReturn(5L);
        when(client.getName()).thenReturn("Constructora ABC");
        when(client.getLegalRepresentative()).thenReturn("Jane Doe");

        when(consultClientService.get(5L))
                .thenReturn(client);

        mockMvc.perform(get("/api/v1/clients/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Constructora ABC"));

        verify(consultClientService).get(5L);
    }

    @Test
    void shouldUpdateClient() throws Exception {

        Client client = mock(Client.class);

        when(client.getId()).thenReturn(5L);
        when(client.getName()).thenReturn("Nombre actualizado");
        when(client.getLegalRepresentative()).thenReturn("Nuevo representante");

        when(updateClientService.update(
                5L,
                "\"client-revision\"",
                "Nombre actualizado",
                "Nuevo representante"
        )).thenReturn(client);

        mockMvc.perform(
                put("/api/v1/clients/5")
                        .header("If-Match", "\"client-revision\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nombre actualizado",
                                  "legalRepresentative": "Nuevo representante"
                                }
                                """)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nombre actualizado"));

        verify(updateClientService).update(
                5L,
                "\"client-revision\"",
                "Nombre actualizado",
                "Nuevo representante"
        );
    }

    @Test
    void shouldRejectInvalidClientUpdate() throws Exception {

        mockMvc.perform(
                put("/api/v1/clients/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "legalRepresentative": "Representante"
                                }
                                """)
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(updateClientService);
    }

    @Test
    void shouldDeleteClient() throws Exception {

        mockMvc.perform(
                delete("/api/v1/clients/5")
                        .header("If-Match", "\"client-revision\"")
        ).andExpect(status().isNoContent());

        verify(deleteClientService).delete(5L, "\"client-revision\"");
    }
}
