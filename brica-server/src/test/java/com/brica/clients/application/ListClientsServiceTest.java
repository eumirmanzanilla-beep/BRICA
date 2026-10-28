package com.brica.clients.application;

import java.util.List;

import com.brica.clients.application.port.ClientStore;
import com.brica.clients.domain.Client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListClientsServiceTest {

    @Mock
    private ClientStore clientStore;

    @InjectMocks
    private ListClientsService listClientsService;

    @Test
    void shouldListClients() {
        List<Client> clients = List.of(
            new Client("Constructora ABC", "Jane Doe"),
            new Client("Constructora XYZ", "John Doe")
        );

        when(clientStore.list())
            .thenReturn(clients);

        List<Client> result =
                listClientsService.listClients();

        assertEquals(clients, result);

        verify(clientStore).list();
    }
}
