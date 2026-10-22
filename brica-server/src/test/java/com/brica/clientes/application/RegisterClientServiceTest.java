package com.brica.clientes.application;

import com.brica.clientes.domain.Cliente;
import com.brica.clientes.infrastructure.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterClientServiceTest {

    @Mock
    private ClienteRepository clientRepository;

    @InjectMocks
    private RegisterClientService registerClientService;

    @Test
    void shouldRegisterClient() {
        when(clientRepository.save(any(Cliente.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Cliente client = registerClientService.registerClient(
            "Constructora ABC",
            "Jane Doe"
        );

        assertEquals(
            "Constructora ABC",
            client.getNombre()
        );

        assertEquals(
            "Jane Doe",
            client.getRepresentanteLegal()
        );

        verify(clientRepository)
            .save(any(Cliente.class));
    }
}