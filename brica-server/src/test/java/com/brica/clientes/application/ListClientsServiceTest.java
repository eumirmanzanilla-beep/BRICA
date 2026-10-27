package com.brica.clientes.application;

import java.util.List;

import com.brica.clientes.domain.Cliente;
import com.brica.clientes.infrastructure.ClienteRepository;

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
    private ClienteRepository clientRepository;

    @InjectMocks
    private ListClientsService listClientsService;

    @Test
    void shouldListClients() {
        List<Cliente> clients = List.of(
            new Cliente("Constructora ABC", "Jane Doe"),
            new Cliente("Constructora XYZ", "John Doe")
        );

        when(clientRepository.findAll())
            .thenReturn(clients);

        List<Cliente> result =
                listClientsService.listClients();

        assertEquals(clients, result);

        verify(clientRepository).findAll();
    }
}