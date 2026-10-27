package com.brica.clients.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 255)
    private String name;

    @Column(name = "representante_legal", nullable = false, length = 255)
    private String legalRepresentative;

    protected Client() {
    }

    public Client(
            String name,
            String legalRepresentative) {

        this.name = name;
        this.legalRepresentative = legalRepresentative;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLegalRepresentative() {
        return legalRepresentative;
    }
}