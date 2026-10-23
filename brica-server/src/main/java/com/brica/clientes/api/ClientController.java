package com.brica.clientes.api;

import com.brica.clientes.application.RegisterClientService;
import com.brica.clientes.domain.Cliente;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final RegisterClientService registerClientService;

    public ClientController(
            RegisterClientService registerClientService) {

        this.registerClientService = registerClientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse registerClient(
            @Valid @RequestBody RegisterClientRequest request) {

        Cliente client = registerClientService.registerClient(
            request.name(),
            request.legalRepresentative()
        );

        return ClientResponse.from(client);
    }
}