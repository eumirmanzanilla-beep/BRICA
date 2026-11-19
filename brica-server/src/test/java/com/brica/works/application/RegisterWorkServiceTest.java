package com.brica.works.application;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brica.clients.application.ClientNotFoundException;
import com.brica.clients.domain.Client;
import com.brica.clients.application.port.ClientStore;
import com.brica.works.domain.Work;
import com.brica.works.application.port.WorkStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterWorkServiceTest {

    @Mock
    private ClientStore clientRepository;

    @Mock
    private WorkStore workRepository;

    @InjectMocks
    private RegisterWorkService registerWorkService;

    @Test
    void shouldRegisterWorkForExistingClient() {
        Client client = new Client(
            "Constructora_ABC",
            "Jane Doe"
        );

        when(clientRepository.findById(1L))
            .thenReturn(Optional.of(client));

        when(workRepository.save(any(Work.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Work work = registerWorkService.registerWork(
            1L,
            "Administrative_Building_Renovation",
            "Merida, Yucatan",
            "General renovation work",
            LocalDate.of(2026, 10, 23),
            LocalDate.of(2027, 1, 30)
        );

        assertEquals(
            "Administrative_Building_Renovation",
            work.getName()
        );

        assertEquals(client, work.getClient());

        verify(clientRepository).findById(1L);
        verify(workRepository).save(any(Work.class));
    }

    @Test
    void shouldRejectWorkWhenClientDoesNotExist() {
        when(clientRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThrows(
            ClientNotFoundException.class,
            () -> registerWorkService.registerWork(
                999L,
                "Invalid Obra",
                "Merida, Yucatan",
                null,
                LocalDate.of(2026, 10, 23),
                null
            )
        );

        verify(clientRepository).findById(999L);

        verify(workRepository, never())
            .save(any(Work.class));
    }
    @Test
    void shouldRejectInvalidClientIdBeforeAccessingPersistence() {
        assertThrows(com.brica.server.shared.validation.domain.InvalidDataException.class,
                () -> registerWorkService.registerWork(0L, "Obra", "Ubicación", null,
                        LocalDate.of(2026, 1, 1), null));
        org.mockito.Mockito.verifyNoInteractions(clientRepository, workRepository);
    }
}
