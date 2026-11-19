package com.brica.extraction.domain;
import com.brica.server.shared.validation.domain.ExtractionRules;

import java.time.OffsetDateTime;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "extraction_jobs")
public class ExtractionJob {
    public enum State { QUEUED, RUNNING, COMPLETED, PARTIAL, FAILED, CANCELLED }
    public enum ReviewState { DRAFT, CONFIRMED, CANCELLED }
    @Id private UUID id;
    @Version @Column(nullable = false) private Long revision;
    @Column(name = "work_id", nullable = false, updatable = false) private Long workId;
    @Column(name = "client_id", nullable = false, updatable = false) private Long clientId;
    @Column(name = "parent_job_id", updatable = false) private UUID parentJobId;
    @Column(name = "review_closed_by", length = 200) private String reviewClosedBy;
    @Column(name = "review_closed_at") private OffsetDateTime reviewClosedAt;
    @Column(name = "request_key", nullable = false, updatable = false) private UUID requestKey;
    @Column(name = "request_hash", nullable = false, length = 64, updatable = false) private String requestHash;
    @Column(name = "actor", nullable = false, updatable = false, length = 200) private String actor;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private State state;
    @Enumerated(EnumType.STRING) @Column(name = "review_state", nullable = false, length = 20) private ReviewState reviewState;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "review_data", nullable = false, columnDefinition = "jsonb") private ExtractionReview review;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "result_data", nullable = false, columnDefinition = "jsonb") private List<SourceResult> results;
    @Column(name = "diagnostic", length = 1000) private String diagnostic;
    @Column(name = "attempt_token") private UUID attemptToken;
    @Column(name = "attempt_number", nullable = false) private int attemptNumber;
    @Column(name = "confirmed_blocks", nullable = false) private int confirmedBlocks;
    @Column(name = "lease_until") private OffsetDateTime leaseUntil;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    protected ExtractionJob() { }
    public ExtractionJob(Long workId, Long clientId, UUID key, String hash, String actor, ExtractionReview review) {
        this(workId,clientId,key,hash,actor,review,null);
    }
    public ExtractionJob(Long workId, Long clientId, UUID key, String hash, String actor, ExtractionReview review,UUID parent) {
        id = UUID.randomUUID(); this.workId = workId; this.clientId = clientId; requestKey = key; requestHash = hash;
        parentJobId = parent;
        this.actor = actor; this.review = review; state = State.QUEUED; reviewState = ReviewState.DRAFT;
        results = List.of(); createdAt = updatedAt = OffsetDateTime.now();
    }
    public void saveReview(ExtractionReview value) { requireDraft(); review = value; touch(); }
    public void requireDraft() { ExtractionRules.require(reviewState == ReviewState.DRAFT, "review", "La revisión ya fue cerrada o cancelada."); }
    public UUID claim() {
        requireDraft();
        ExtractionRules.require(state == State.QUEUED || state == State.RUNNING && leaseUntil.isBefore(OffsetDateTime.now()), "state", "El trabajo no está disponible.");
        state = State.RUNNING; attemptToken = UUID.randomUUID(); attemptNumber++;
        leaseUntil = OffsetDateTime.now().plusSeconds(30); touch(); return attemptToken;
    }
    public boolean owns(UUID token) { return reviewState == ReviewState.DRAFT && state == State.RUNNING && Objects.equals(attemptToken, token) && leaseUntil != null && leaseUntil.isAfter(OffsetDateTime.now()); }
    public void finish(UUID token, List<SourceResult> values, String error) {
        if (!owns(token)) { return; }
        results = List.copyOf(values); diagnostic = error;
        state = error == null ? State.COMPLETED : values.isEmpty() ? State.FAILED : State.PARTIAL;
        leaseUntil = null; touch();
    }
    public void retry() {
        requireDraft(); ExtractionRules.require(state != State.RUNNING, "state", "Cancela o espera el análisis activo.");
        ExtractionRules.require(results.isEmpty() && review.decisions().isEmpty() && review.budgets().isEmpty(), "review", "Conserva este intento y crea otro trabajo para no perder evidencia o decisiones.");
        state = State.QUEUED; diagnostic = null; touch();
    }
    public void cancel(String actor) {
        requireDraft(); closedBy(actor); reviewState = ReviewState.CANCELLED;
        if (state == State.QUEUED || state == State.RUNNING) { state = State.CANCELLED; }
        attemptToken = null; leaseUntil = null; touch();
    }
    public void closeReview(String actor) {
        requireDraft(); closedBy(actor); reviewState = ReviewState.CONFIRMED;
        if (state == State.QUEUED || state == State.RUNNING) { state = State.CANCELLED; }
        attemptToken = null; leaseUntil = null; touch();
    }
    private void closedBy(String actor) { ExtractionRules.text("actor",actor,200,true); reviewClosedBy=actor; reviewClosedAt=OffsetDateTime.now(); }
    public void recordConfirmation() { requireDraft(); confirmedBlocks++; touch(); }
    public int getConfirmedBlocks() { return confirmedBlocks; }
    private void touch() { updatedAt = OffsetDateTime.now(); }
    public UUID getId() { return id; } public Long getWorkId() { return workId; } public Long getClientId() { return clientId; }
    public UUID getRequestKey() { return requestKey; } public String getRequestHash() { return requestHash; }
    public UUID getParentJobId() { return parentJobId; }
    public String getReviewClosedBy() { return reviewClosedBy; } public OffsetDateTime getReviewClosedAt() { return reviewClosedAt; }
    public String getActor() { return actor; } public Long getRevision() { return revision; }
    public State getState() { return state; } public ReviewState getReviewState() { return reviewState; }
    public ExtractionReview getReview() { return review; } public List<SourceResult> getResults() { return results; }
    public String getDiagnostic() { return diagnostic; } public UUID getAttemptToken() { return attemptToken; }
    public int getAttemptNumber() { return attemptNumber; } public OffsetDateTime getCreatedAt() { return createdAt; }
    public String etag() { return "\"extraction-" + id + "-" + revision + "\""; }
}
