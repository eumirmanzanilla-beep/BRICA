package com.brica.works.domain;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import com.brica.server.shared.validation.domain.InputRules;

import com.brica.clients.domain.Client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "works")
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_work")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "description")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    protected Work() {
    }

    public Work(
            Client client,
            String name,
            String location,
            String description,
            LocalDate startDate,
            LocalDate endDate) {

        validate(name, location, startDate, endDate);
        if (client == null) {
            throw new com.brica.server.shared.validation.domain.InvalidDataException(
                    java.util.Map.of("clientId", "El cliente es obligatorio."));
        }
        this.client = client;
        this.name = name;
        this.location = location;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
    private static void validate(String name, String location, LocalDate start, LocalDate end) {
        var errors = new LinkedHashMap<String, String>();
        InputRules.requiredText(errors, "name", name, 255);
        InputRules.requiredText(errors, "location", location, 255);
        if (start == null) {
            errors.put("startDate", "La fecha de inicio es obligatoria.");
        } else if (end != null && end.isBefore(start)) {
            errors.put("endDate", "La fecha de terminación no puede ser anterior al inicio.");
        }
        InputRules.rejectErrors(errors);
    }

    public void correctGeneralInformation(String name, String location, String description) {
        validate(name, location, startDate, endDate);
        this.name = name;
        this.location = location;
        this.description = description;
    }
}
