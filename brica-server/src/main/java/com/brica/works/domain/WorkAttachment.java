package com.brica.works.domain;

import java.time.OffsetDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "work_attachments")
public class WorkAttachment {
    public enum Kind { CONTRACT, BASE_BUDGET }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_attachment") private Long id;
    @Column(name = "work_id", nullable = false) private Long workId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Kind kind;
    @Column(name = "original_name", nullable = false, length = 255) private String originalName;
    @Column(name = "mime_type", nullable = false, length = 100) private String mimeType;
    @Column(name = "relative_path", nullable = false, length = 500) private String relativePath;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(nullable = false, length = 64) private String sha256;
    @Column(name = "registered_at", nullable = false) private OffsetDateTime registeredAt;
    protected WorkAttachment() { }
    public WorkAttachment(Long workId, Kind kind, String name, String mime, String path, long size, String sha256) {
        this.workId = workId; this.kind = kind; this.originalName = name; this.mimeType = mime;
        this.relativePath = path; this.sizeBytes = size; this.sha256 = sha256; this.registeredAt = OffsetDateTime.now();
    }
    public Long getId() { return id; }
    public Long getWorkId() { return workId; }
    public Kind getKind() { return kind; }
    public String getOriginalName() { return originalName; }
    public String getMimeType() { return mimeType; }
    public String getRelativePath() { return relativePath; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public OffsetDateTime getRegisteredAt() { return registeredAt; }
}
