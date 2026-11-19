package com.brica.extraction.domain;

import java.util.UUID;
import jakarta.persistence.*;

@Entity @Table(name = "extraction_sources")
public class ExtractionSource {
    @Id private UUID id;
    @Column(name = "job_id", nullable = false, updatable = false) private UUID jobId;
    @Column(name = "attachment_id", nullable = false, updatable = false) private Long attachmentId;
    @Column(name = "work_id", nullable = false, updatable = false) private Long workId;
    @Column(name = "source_order", nullable = false, updatable = false) private int order;
    @Column(name = "sha256", nullable = false, updatable = false, length = 64) private String sha256;
    protected ExtractionSource() { }
    public ExtractionSource(UUID jobId, Long attachmentId, Long workId, int order, String sha256) {
        id = UUID.randomUUID(); this.jobId = jobId; this.attachmentId = attachmentId; this.workId = workId; this.order = order; this.sha256 = sha256;
    }
    public UUID getId() { return id; } public UUID getJobId() { return jobId; }
    public Long getAttachmentId() { return attachmentId; } public Long getWorkId() { return workId; }
    public String getSha256() { return sha256; }
}
