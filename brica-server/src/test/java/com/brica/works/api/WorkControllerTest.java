
package com.brica.works.api;

import java.time.LocalDate;
import java.util.List;

import com.brica.clients.application.ClientNotFoundException;
import com.brica.clients.domain.Client;
import com.brica.server.shared.error.api.ApiExceptionHandler;
import com.brica.works.application.ConsultWorkService;
import com.brica.works.application.CorrectWorkService;
import com.brica.works.application.DeleteWorkService;
import com.brica.works.application.ListWorksService;
import com.brica.works.application.RegisterWorkService;
import com.brica.works.domain.Work;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkController.class)
@org.springframework.security.test.context.support.WithMockUser
@Import({ApiExceptionHandler.class, com.brica.server.security.infrastructure.SecurityConfiguration.class,
        com.brica.server.security.infrastructure.PasswordConfiguration.class})
class WorkControllerTest {
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
    private RegisterWorkService registerWorkService;

    @MockitoBean
    private ListWorksService listWorksService;

    @MockitoBean
    private ConsultWorkService consultService;

    @MockitoBean
    private CorrectWorkService correctService;

    @MockitoBean
    private DeleteWorkService deleteWorkService;
    @MockitoBean private com.brica.works.application.DocumentInformationService documentInformationService;

    @Test
    void shouldListWorks() throws Exception {

        Client client = mock(Client.class);
        when(client.getId()).thenReturn(1L);

        Work work = mock(Work.class);
        when(work.getId()).thenReturn(10L);
        when(work.getClient()).thenReturn(client);
        when(work.getName()).thenReturn("Administrative Building Renovation");
        when(work.getLocation()).thenReturn("Merida, Yucatan");
        when(work.getDescription()).thenReturn("General renovation work");
        when(work.getStartDate()).thenReturn(LocalDate.of(2026, 10, 23));
        when(work.getEndDate()).thenReturn(LocalDate.of(2027, 1, 30));

        when(listWorksService.listWorks())
                .thenReturn(List.of(work));

        mockMvc.perform(get("/api/v1/works"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].clientId").value(1))
                .andExpect(jsonPath("$[0].name")
                        .value("Administrative Building Renovation"))
                .andExpect(jsonPath("$[0].location").value("Merida, Yucatan"))
                .andExpect(jsonPath("$[0].description").value("General renovation work"))
                .andExpect(jsonPath("$[0].startDate").value("2026-10-23"))
                .andExpect(jsonPath("$[0].endDate").value("2027-01-30"));

        verify(listWorksService).listWorks();
    }

    @Test
    void shouldRegisterWork() throws Exception {

        Client client = mock(Client.class);
        when(client.getId()).thenReturn(1L);

        Work work = mock(Work.class);
        when(work.getId()).thenReturn(10L);
        when(work.getClient()).thenReturn(client);
        when(work.getName()).thenReturn("Administrative Building Renovation");
        when(work.getLocation()).thenReturn("Merida, Yucatan");
        when(work.getDescription()).thenReturn("General renovation work");
        when(work.getStartDate()).thenReturn(LocalDate.of(2026, 10, 23));
        when(work.getEndDate()).thenReturn(LocalDate.of(2027, 1, 30));

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
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.clientId").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Administrative Building Renovation"))
                .andExpect(jsonPath("$.location").value("Merida, Yucatan"));
    }

    @Test
    void shouldRejectWorkWhenClientDoesNotExist() throws Exception {

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
                .andExpect(jsonPath("$.title").value("Cliente no encontrado"));
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
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(registerWorkService);
    }

    @Test
    void shouldDeleteWork() throws Exception {

        mockMvc.perform(
                delete("/api/v1/works/10")
        ).andExpect(status().isNoContent());

        verify(deleteWorkService).delete(10L);
    }
}
