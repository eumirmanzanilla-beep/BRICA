
package com.brica.clients.domain;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Objects;

import com.brica.server.shared.validation.domain.InputRules;

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

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    protected Client() {
    }

    public Client(String name, String legalRepresentative) {
        updateDetails(name, legalRepresentative);
    }

    public void updateDetails(
            String name,
            String legalRepresentative) {

        var errors = new LinkedHashMap<String, String>();

        InputRules.requiredText(errors, "name", name, 255);
        InputRules.requiredText(
                errors,
                "legalRepresentative",
                legalRepresentative,
                255
        );

        InputRules.rejectErrors(errors);

        this.name = name;
        this.legalRepresentative = legalRepresentative;
    }

    public void markDeleted(OffsetDateTime deletedAt) {
        this.deletedAt = Objects.requireNonNull(
                deletedAt,
                "La fecha de eliminación es obligatoria."
        );
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

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }
}
