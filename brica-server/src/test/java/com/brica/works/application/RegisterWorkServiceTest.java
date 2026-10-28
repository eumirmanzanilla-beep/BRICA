package com.brica.works.application;

import java.time.LocalDate;
import java.util.Optional;

import com.brica.clients.application.ClientNotFoundException;
import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;
import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterWorkServiceTest {

    @Mock
    private ClientStore clientStore;

    @Mock
    private WorkStore workStore;

    @InjectMocks
    private RegisterWorkService registerWorkService;

    @Test
    void shouldRegisterWorkForExistingClient() {
        Client client = new Client(
            "Constructora ABC",
            "Jane Doe"
        );

        when(clientStore.findById(1L))
            .thenReturn(Optional.of(client));

        when(workStore.save(any(Work.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Work work = registerWorkService.registerWork(
            1L,
            "Administrative Building Renovation",
            "Merida, Yucatan",
            "General renovation work",
            LocalDate.of(2026, 10, 23),
            LocalDate.of(2027, 1, 30)
        );

        assertEquals(
            "Administrative Building Renovation",
            work.getName()
        );

        assertEquals(client, work.getClient());

        verify(clientStore).findById(1L);
        verify(workStore).save(any(Work.class));
    }

    @Test
    void shouldRejectWorkWhenClientDoesNotExist() {
        when(clientStore.findById(999L))
            .thenReturn(Optional.empty());

        assertThrows(
            ClientNotFoundException.class,
            () -> registerWorkService.registerWork(
                999L,
                "Invalid Work",
                "Merida, Yucatan",
                null,
                LocalDate.of(2026, 10, 23),
                null
            )
        );

        verify(clientStore).findById(999L);

        verify(workStore, never())
            .save(any(Work.class));
    }
}
