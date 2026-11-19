package com.brica.budgets.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "budget_revisions")
public class BudgetRevision {
    @Id private UUID id;
    @Column(name = "budget_id", nullable = false, updatable = false) private Long budgetId;
    @Column(name = "work_id", nullable = false, updatable = false) private Long workId;
    @Column(name = "revision_number", nullable = false, updatable = false) private int number;
    @Column(name = "job_id", nullable = false, updatable = false) private UUID jobId;
    @Column(name = "confirmation_id", nullable = false, updatable = false) private UUID confirmationId;
    @Column(name = "currency", length = 3, updatable = false) private String currency;
    @Column(columnDefinition = "numeric", updatable = false) private BigDecimal subtotal;
    @Column(columnDefinition = "numeric", updatable = false) private BigDecimal tax;
    @Column(columnDefinition = "numeric", updatable = false) private BigDecimal total;
    @Column(name = "justification", length = 2000, updatable = false) private String justification;
    @Column(name = "control_policy", nullable = false, updatable = false, length = 30) private String controlPolicy;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "notes", nullable = false, columnDefinition = "jsonb", updatable = false) private List<String> notes;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "differences", nullable = false, columnDefinition = "jsonb", updatable = false) private List<BudgetDraft.Difference> differences;
    @Column(name = "confirmed_at", nullable = false, updatable = false) private OffsetDateTime confirmedAt;
    protected BudgetRevision() { }
    public BudgetRevision(Long budgetId, Long workId, int number, UUID job, UUID confirmation, BudgetDraft draft) {
        id = UUID.randomUUID(); this.budgetId = budgetId; this.workId = workId; this.number = number; jobId = job; confirmationId = confirmation;
        currency = draft.currency(); subtotal = draft.subtotal(); tax = draft.tax(); total = draft.total(); justification = draft.justification();
        controlPolicy = "CONTROL_V1"; notes = draft.notes(); differences = draft.validate(); confirmedAt = OffsetDateTime.now();
    }
    public UUID getId() { return id; } public Long getBudgetId() { return budgetId; } public int getNumber() { return number; }
    public UUID getJobId() { return jobId; } public String getCurrency() { return currency; }
    public BigDecimal getSubtotal() { return subtotal; } public BigDecimal getTax() { return tax; } public BigDecimal getTotal() { return total; }
    public List<String> getNotes() { return notes; } public List<BudgetDraft.Difference> getDifferences() { return differences; }
    public String getControlPolicy() { return controlPolicy; } public String getJustification() { return justification; }
}
