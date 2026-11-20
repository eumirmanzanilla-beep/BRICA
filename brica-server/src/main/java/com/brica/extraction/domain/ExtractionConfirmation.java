package com.brica.extraction.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.persistence.*;

@Entity @Table(name = "extraction_confirmations")
public class ExtractionConfirmation {
    @Id private UUID id;
    @Column(name = "job_id", nullable = false, updatable = false) private UUID jobId;
    @Column(name = "operation_key", nullable = false, updatable = false) private UUID operationKey;
    @Column(name = "request_hash", nullable = false, length = 64, updatable = false) private String requestHash;
    @Column(name = "block_key", nullable = false, length = 100, updatable = false) private String blockKey;
    @Column(name = "actor", nullable = false, length = 200, updatable = false) private String actor;
    @Column(name = "result", nullable = false, updatable = false, length = 1000) private String result;
    @Column(name = "audit_json", nullable = false, updatable = false, columnDefinition = "text") private String auditJson;
    @Column(name = "confirmed_at", nullable = false, updatable = false) private OffsetDateTime confirmedAt;
    protected ExtractionConfirmation() { }
    public ExtractionConfirmation(UUID jobId, UUID key, String hash, String block, String actor, String result, String audit) {
        this(UUID.randomUUID(), jobId, key, hash, block, actor, result, audit);
    }
    public ExtractionConfirmation(UUID id, UUID jobId, UUID key, String hash, String block, String actor, String result, String audit) {
        this.id = id; this.jobId = jobId; operationKey = key; requestHash = hash; blockKey = block;
        this.actor = actor; this.result = result; auditJson = audit; confirmedAt = OffsetDateTime.now();
    }
    public UUID getId() { return id; } public UUID getOperationKey() { return operationKey; }
    public String getRequestHash() { return requestHash; } public String getBlockKey() { return blockKey; }
    public String getActor() { return actor; } public String getResult() { return result; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
}
