package com.brica.documents.domain;

import java.time.OffsetDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "document_files")
public class DocumentFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_file")
    private Long id;
    @Column(name = "original_name", nullable = false, length = 255, updatable = false)
    private String originalName;
    @Column(name = "relative_path", nullable = false, length = 500, updatable = false)
    private String relativePath;
    @Column(name = "mime_type", nullable = false, length = 100, updatable = false)
    private String mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(nullable = false, length = 64, updatable = false)
    private String sha256;
    @Column(name = "registered_at", nullable = false, updatable = false)
    private OffsetDateTime registeredAt = OffsetDateTime.now();

    protected DocumentFile() { }
    public DocumentFile(String name, String path, long size, String hash) {
        originalName = name;
        relativePath = path;
        sizeBytes = size;
        sha256 = hash;
    }
    public Long getId() { return id; }
    public String getOriginalName() { return originalName; }
    public String getRelativePath() { return relativePath; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
}
