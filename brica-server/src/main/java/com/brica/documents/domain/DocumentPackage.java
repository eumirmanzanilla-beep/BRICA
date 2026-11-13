package com.brica.documents.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.brica.server.shared.validation.domain.InvalidDataException;
import jakarta.persistence.*;

@Entity
@Table(name = "document_packages")
public class DocumentPackage {
    public enum State { DRAFT, CONFIRMED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_package")
    private Long id;
    @Column(name = "client_id")
    private Long clientId;
    @Column(name = "work_id")
    private Long workId;
    @Column(length = 150)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private State state = State.DRAFT;
    @Version @Column(nullable = false)
    private Long revision;
    @ElementCollection
    @org.hibernate.annotations.BatchSize(size = 50)
    @CollectionTable(name = "document_package_templates", joinColumns = @JoinColumn(name = "package_id"))
    @Column(name = "template_id", nullable = false)
    private Set<Long> templateIds = new HashSet<>();

    public DocumentPackage() { }
    public void scope(Long workId) {
        if (state == State.CONFIRMED && !java.util.Objects.equals(this.workId, workId)) {
            throw new DocumentRuleException("PACKAGE_SCOPE_FIXED", "El ámbito de un paquete confirmado no puede cambiar.");
        }
        this.workId = workId;
    }
    public Long getWorkId() { return workId; }
    public void edit(Long clientId, String name, List<Long> templates, boolean confirm) {
        if (state == State.CONFIRMED && !java.util.Objects.equals(this.clientId, clientId)) {
            throw new DocumentRuleException("PACKAGE_CLIENT_FIXED", "El cliente de un paquete confirmado no puede cambiar.");
        }
        if (templates == null || templates.stream().anyMatch(java.util.Objects::isNull)
                || new HashSet<>(templates).size() != templates.size() || templates.stream().anyMatch(id -> id <= 0)) {
            throw new InvalidDataException(java.util.Map.of("templateIds", "Selecciona plantillas distintas y válidas."));
        }
        if (name != null && name.length() > 150) {
            throw new InvalidDataException(java.util.Map.of("name", "El nombre no debe superar 150 caracteres."));
        }
        if (confirm || state == State.CONFIRMED) {
            if (clientId == null || name == null || name.isBlank() || templates.isEmpty()) {
                throw new InvalidDataException(java.util.Map.of("package", "Indica cliente, nombre y al menos una plantilla elegible."));
            }
            state = State.CONFIRMED;
        }
        this.clientId = clientId;
        this.name = name;
        templateIds.clear();
        templateIds.addAll(templates);
    }
    public Long getId() { return id; }
    public Long getClientId() { return clientId; }
    public String getName() { return name; }
    public State getState() { return state; }
    public Long getRevision() { return revision; }
    public Set<Long> getTemplateIds() { return Set.copyOf(templateIds); }
}
