package com.brica.clients.application;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brica.clients.application.ListClientsService;
import com.brica.clients.domain.Client;
import com.brica.clients.infrastructure.ClientRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListClientsServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ListClientsService listClientsService;

    @Test
    void shouldListClients() {
        List<Client> clients = List.of(
            new Client("Constructora ABC", "Jane Doe"),
            new Client("Constructora XYZ", "John Doe")
        );

        when(clientRepository.findAll())
            .thenReturn(clients);

        List<Client> result =
                listClientsService.listClients();

        assertEquals(clients, result);

        verify(clientRepository).findAll();
    }
}