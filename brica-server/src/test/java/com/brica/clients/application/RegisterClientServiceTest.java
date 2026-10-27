package com.brica.clients.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brica.clients.application.RegisterClientService;
import com.brica.clients.domain.Client;
import com.brica.clients.infrastructure.ClientRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private RegisterClientService registerClientService;

    @Test
    void shouldRegisterClient() {
        when(clientRepository.save(any(Client.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Client client = registerClientService.registerClient(
            "Constructora ABC",
            "Jane Doe"
        );

        assertEquals(
            "Constructora ABC",
            client.getName()
        );

        assertEquals(
            "Jane Doe",
            client.getLegalRepresentative()
        );

        verify(clientRepository)
            .save(any(Client.class));
    }
}