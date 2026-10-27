package com.brica.clients.api;

import java.util.List;

import com.brica.clients.application.ListClientsService;
import com.brica.clients.application.RegisterClientService;
import com.brica.clients.domain.Client;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final RegisterClientService registerClientService;
    private final ListClientsService listClientsService;

    public ClientController(
            RegisterClientService registerClientService,
            ListClientsService listClientsService) {

        this.registerClientService = registerClientService;
        this.listClientsService = listClientsService;
    }

    @GetMapping
    public List<ClientResponse> listClients() {
        return listClientsService.listClients()
                .stream()
                .map(ClientResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientResponse registerClient(
            @Valid @RequestBody RegisterClientRequest request) {

        Client client = registerClientService.registerClient(
            request.name(),
            request.legalRepresentative()
        );

        return ClientResponse.from(client);
    }
}