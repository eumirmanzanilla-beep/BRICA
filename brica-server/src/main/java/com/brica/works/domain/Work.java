
package com.brica.works.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.math.BigDecimal;
import java.util.Map;

import com.brica.clients.domain.Client;
import com.brica.server.shared.validation.domain.InputRules;
import com.brica.server.shared.validation.domain.InvalidDataException;

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

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "description")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "contract_number", length = 50)
    private String contractNumber;
    @Column(name = "contractor", length = 200)
    private String contractor;
    @Column(name = "contracted_amount", columnDefinition = "numeric")
    private BigDecimal contractedAmount;
    @Column(name = "amount_before_tax", columnDefinition = "numeric")
    private BigDecimal amountBeforeTax;
    @Column(name = "tax_amount", columnDefinition = "numeric")
    private BigDecimal taxAmount;
    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkStatus status = WorkStatus.IN_PROGRESS;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "additional_values", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> additionalValues = new LinkedHashMap<>();

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

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
            throw new InvalidDataException(
                    java.util.Map.of(
                            "clientId",
                            "El cliente es obligatorio."
                    )
            );
        }

        this.client = client;
        this.name = name;
        this.location = location;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void correctGeneralInformation(
            String name,
            String location,
            String description) {

        validate(name, location, startDate, endDate);

        this.name = name;
        this.location = location;
        this.description = description;
    }

    public void updateDocumentInformation(String contractNumber, String contractor, BigDecimal contractedAmount,
            BigDecimal amountBeforeTax, BigDecimal taxAmount, LocalDate startDate, LocalDate endDate, LocalDate actualEndDate, WorkStatus status,
            Map<String, String> additionalValues) {
        var errors = new LinkedHashMap<String, String>();
        if (contractNumber != null && contractNumber.length() > 50) { errors.put("contractNumber", "Máximo 50 caracteres."); }
        if (contractor != null && contractor.length() > 200) { errors.put("contractor", "Máximo 200 caracteres."); }
        if (status == null) { errors.put("status", "Selecciona el estado de la obra."); }
        if (additionalValues == null) { errors.put("additionalValues", "Indica los valores adicionales."); }
        InputRules.rejectErrors(errors);
        validate(name, location, startDate, endDate);
        this.contractNumber = contractNumber;
        this.contractor = contractor;
        this.contractedAmount = contractedAmount;
        this.amountBeforeTax = amountBeforeTax;
        this.taxAmount = taxAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.actualEndDate = actualEndDate;
        this.status = status;
        this.additionalValues = new LinkedHashMap<>(additionalValues);
    }

    public void markDeleted(OffsetDateTime deletedAt) {
        this.deletedAt = Objects.requireNonNull(
                deletedAt,
                "La fecha de eliminación es obligatoria."
        );
    }

    private static void validate(
            String name,
            String location,
            LocalDate start,
            LocalDate end) {

        var errors = new LinkedHashMap<String, String>();

        InputRules.requiredText(errors, "name", name, 255);
        if (location != null && location.length() > 255) { errors.put("location", "Máximo 255 caracteres."); }

        if (start != null && end != null && end.isBefore(start)) {
            errors.put(
                    "endDate",
                    "La fecha de terminación no puede ser anterior al inicio."
            );
        }

        InputRules.rejectErrors(errors);
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

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public String getContractNumber() { return contractNumber; }
    public String getContractor() { return contractor; }
    public BigDecimal getContractedAmount() { return contractedAmount; }
    public BigDecimal getAmountBeforeTax() { return amountBeforeTax; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public LocalDate getActualEndDate() { return actualEndDate; }
    public WorkStatus getStatus() { return status; }
    public Map<String, String> getAdditionalValues() { return Map.copyOf(additionalValues); }
}
