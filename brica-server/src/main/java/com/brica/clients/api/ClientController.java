
package com.brica.clients.api;

import java.util.List;

import com.brica.clients.application.ConsultClientService;
import com.brica.clients.application.DeleteClientService;
import com.brica.clients.application.ListClientsService;
import com.brica.clients.application.RegisterClientService;
import com.brica.clients.application.UpdateClientService;
import com.brica.clients.domain.Client;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final RegisterClientService registerClientService;
    private final ListClientsService listClientsService;
    private final ConsultClientService consultClientService;
    private final UpdateClientService updateClientService;
    private final DeleteClientService deleteClientService;

    public ClientController(
            RegisterClientService registerClientService,
            ListClientsService listClientsService,
            ConsultClientService consultClientService,
            UpdateClientService updateClientService,
            DeleteClientService deleteClientService) {

        this.registerClientService = registerClientService;
        this.listClientsService = listClientsService;
        this.consultClientService = consultClientService;
        this.updateClientService = updateClientService;
        this.deleteClientService = deleteClientService;
    }

    @GetMapping
    public List<ClientResponse> listClients(
            @RequestParam(value = "page", required = false)
            Integer page,

            @RequestParam(value = "size", required = false)
            Integer size) {

        List<Client> clients;

        if (page == null && size == null) {
            clients = listClientsService.listClients();
        } else {
            clients = listClientsService.listClients(
                    page == null ? 0 : page,
                    size == null ? 50 : size
            );
        }

        return clients.stream()
                .map(ClientResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ClientResponse getClient(
            @PathVariable("id") Long id) {

        Client client = consultClientService.get(id);

        return ClientResponse.from(client);
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

    @PutMapping("/{id}")
    public ClientResponse updateClient(
            @PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false) String expected,
            @Valid @RequestBody UpdateClientRequest request) {

        Client client = updateClientService.update(
                id,
                expected,
                request.name(),
                request.legalRepresentative()
        );

        return ClientResponse.from(client);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClient(
            @PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestHeader(value = "If-Match", required = false) String expected) {

        deleteClientService.delete(id, expected);
    }
    public record DocumentNameRequest(@com.fasterxml.jackson.annotation.JsonProperty(required=true) String documentName) { }
    @PutMapping("/{id}/document-name")
    public ClientResponse documentName(@PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestHeader(value="If-Match",required=false) String expected,
            @RequestBody DocumentNameRequest request) {
        return ClientResponse.from(updateClientService.updateDocumentName(id,expected,request.documentName()));
    }
}
