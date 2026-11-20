package com.brica.budgets.domain;
import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "budget_sections")
public class BudgetSection {
    @Id private UUID id;
    @Column(name = "revision_id", nullable = false, updatable = false) private UUID revisionId;
    @Column(nullable = false, updatable = false) private int position;
    @Column(nullable = false, updatable = false, length = 1000) private String title;
    protected BudgetSection() { }
    public BudgetSection(UUID revision, BudgetDraft.Section value) { id = UUID.randomUUID(); revisionId = revision; position = value.position(); title = value.title(); }
    public UUID getId() { return id; } public int getPosition() { return position; } public String getTitle() { return title; }
}
