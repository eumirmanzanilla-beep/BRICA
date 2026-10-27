package com.brica.clients.infrastructure;

import com.brica.clients.domain.Client;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository
        extends JpaRepository<Client, Long> {
}