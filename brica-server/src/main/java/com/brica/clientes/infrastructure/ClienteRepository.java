package com.brica.clientes.infrastructure;

import com.brica.clientes.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository
        extends JpaRepository<Cliente, Long> {
}