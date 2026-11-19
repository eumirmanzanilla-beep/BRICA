package com.brica.extraction.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Durable technical history; a successful read never implies business confirmation. */
@Entity @Table(name="extraction_attempts")
public class ExtractionAttempt {
    public enum Outcome { RUNNING, COMPLETED, PARTIAL, FAILED, CANCELLED, EXPIRED, LEGACY_UNKNOWN }
    @Id private UUID id;
    @Column(name="job_id",nullable=false,updatable=false) private UUID jobId;
    @Column(name="attempt_number",nullable=false,updatable=false) private int number;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Outcome outcome;
    @Column(name="started_at",updatable=false) private OffsetDateTime startedAt;
    @Column(name="heartbeat_at") private OffsetDateTime heartbeatAt;
    @Column(name="finished_at") private OffsetDateTime finishedAt;
    @Column(length=1000) private String diagnostic;
    @Column(name="result_hash",length=64) private String resultHash;
    @Column(name="reader_version",length=400) private String readerVersion;
    @Column(name="legacy_imported",nullable=false,updatable=false) private boolean legacyImported;
    protected ExtractionAttempt() { }
    public ExtractionAttempt(UUID id,UUID job,int number) {
        this.id=id; jobId=job; this.number=number; outcome=Outcome.RUNNING; startedAt=heartbeatAt=OffsetDateTime.now();
    }
    public UUID getId() { return id; } public int getNumber() { return number; } public Outcome getOutcome() { return outcome; }
    public OffsetDateTime getStartedAt() { return startedAt; } public OffsetDateTime getHeartbeatAt() { return heartbeatAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; } public String getDiagnostic() { return diagnostic; }
    public String getResultHash() { return resultHash; } public String getReaderVersion() { return readerVersion; }
    public boolean isLegacyImported() { return legacyImported; }
}
