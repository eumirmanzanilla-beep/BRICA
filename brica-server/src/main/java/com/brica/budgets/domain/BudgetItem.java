package com.brica.budgets.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "budget_items")
public class BudgetItem {
    @Id private UUID id;
    @Column(name = "revision_id", nullable = false, updatable = false) private UUID revisionId;
    @Column(name = "section_id", updatable = false) private UUID sectionId;
    @Column(nullable = false, updatable = false) private int position;
    @Column(length = 255, updatable = false) private String code;
    @Column(nullable = false, columnDefinition = "text", updatable = false) private String description;
    @Column(length = 255, updatable = false) private String unit;
    @Column(columnDefinition = "numeric", updatable = false) private BigDecimal quantity;
    @Column(columnDefinition = "numeric", updatable = false) private BigDecimal price;
    @Column(columnDefinition = "numeric", updatable = false) private BigDecimal amount;
    @Column(name = "evidence_id", updatable = false) private UUID evidenceId;
    @Column(name = "original_measurement", updatable = false, length = 1000) private String originalMeasurement;
    protected BudgetItem() { }
    public BudgetItem(UUID revision, UUID sectionId, BudgetDraft.Item value) {
        id = UUID.randomUUID(); revisionId = revision; this.sectionId = sectionId; position = value.position();
        code = value.code(); description = value.description(); unit = value.unit(); quantity = value.quantity();
        price = value.price(); amount = value.amount(); evidenceId = value.evidenceId(); originalMeasurement = value.originalMeasurement();
    }
    public UUID getId() { return id; } public UUID getSectionId() { return sectionId; } public int getPosition() { return position; }
    public String getCode() { return code; } public String getDescription() { return description; } public String getUnit() { return unit; }
    public BigDecimal getQuantity() { return quantity; } public BigDecimal getPrice() { return price; } public BigDecimal getAmount() { return amount; }
    public UUID getEvidenceId() { return evidenceId; } public String getOriginalMeasurement() { return originalMeasurement; }
}
