package com.brica.documents.domain;

import java.util.UUID;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "document_registrations")
public class RegistrationOperation {
    public enum Status { PENDING, COMPLETED, FAILED }
    @Id @Column(name = "confirmation_id")
    private UUID id;
    @Column(name = "template_id", nullable = false, updatable = false)
    private Long templateId;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "request_hash", nullable = false, length = 64, updatable = false)
    private String requestHash;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(name = "file_hash", nullable = false, length = 64, updatable = false)
    private String fileHash;
    @Column(name = "original_name", nullable = false, length = 255, updatable = false)
    private String originalName;
    @Column(name = "version_name", nullable = false, length = 150, updatable = false)
    private String versionName = "Version_Pendiente";
    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "analysis_json", nullable = false, updatable = false)
    private String analysisJson;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "requirements_json", nullable = false, updatable = false)
    private String requirementsJson;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;
    @Column(name = "version_number")
    private Integer versionNumber;
    @Column(length = 255)
    private String failure;
    @Version @Column(nullable = false)
    private Long revision;

    protected RegistrationOperation() { }
    public RegistrationOperation(UUID id, Long templateId, String requestHash, String fileHash,
            String name, long size, String analysisJson, String requirementsJson) {
        this.id = id;
        this.templateId = templateId;
        this.requestHash = requestHash;
        this.fileHash = fileHash;
        this.originalName = name;
        this.sizeBytes = size;
        this.analysisJson = analysisJson;
        this.requirementsJson = requirementsJson;
    }
    public void complete(int number) { versionNumber = number; status = Status.COMPLETED; }
    public void nameVersion(String name) { this.versionName = name; }
    public String getVersionName() { return versionName; }
    public void fail() {
        status = Status.FAILED;
        failure = "No se pudo verificar el archivo o la configuración. Revisa la causa e inicia otra confirmación.";
    }
    public UUID getId() { return id; }
    public Long getTemplateId() { return templateId; }
    public String getRequestHash() { return requestHash; }
    public String getFileHash() { return fileHash; }
    public String getOriginalName() { return originalName; }
    public long getSizeBytes() { return sizeBytes; }
    public String getAnalysisJson() { return analysisJson; }
    public String getRequirementsJson() { return requirementsJson; }
    public Status getStatus() { return status; }
    public Integer getVersionNumber() { return versionNumber; }
    public String getFailure() { return failure; }
}
