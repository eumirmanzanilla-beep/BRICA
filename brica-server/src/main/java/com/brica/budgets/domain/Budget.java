package com.brica.budgets.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name = "budgets")
public class Budget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable = false) private Long revision;
    @Column(name = "work_id", nullable = false, updatable = false) private Long workId;
    @Column(nullable = false, length = 255) private String name;
    @Column(name = "principal_revision_id") private UUID principalRevisionId;
    @Column(name = "next_number", nullable = false) private int nextNumber;
    protected Budget() { }
    public Budget(Long workId, String name) { this.workId = workId; this.name = name; nextNumber = 1; }
    public int allocateNumber() { return nextNumber++; }
    public void selectPrincipal(UUID id) { principalRevisionId = id; }
    public Long getId() { return id; } public Long getWorkId() { return workId; }
    public String getName() { return name; } public Long getRevision() { return revision; }
    public UUID getPrincipalRevisionId() { return principalRevisionId; }
    public String etag() { return "\"budget-" + id + "-" + revision + "\""; }
}
