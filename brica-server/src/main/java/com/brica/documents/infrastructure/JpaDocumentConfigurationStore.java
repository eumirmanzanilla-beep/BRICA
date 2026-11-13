package com.brica.documents.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.brica.documents.application.DocumentException;
import com.brica.documents.application.port.DocumentConfigurationStore;
import com.brica.documents.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDocumentConfigurationStore implements DocumentConfigurationStore {
    private final EntityManager em;
    public JpaDocumentConfigurationStore(EntityManager em) { this.em = em; }

    @Override public DocumentTemplate template(Long id, boolean lock) {
        var value = em.find(DocumentTemplate.class, id, lock ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE);
        if (value == null) { throw missing(); }
        return value;
    }
    @Override public List<DocumentTemplate> templates(int page, int size, boolean includeDeleted) {
        return em.createQuery("select t from DocumentTemplate t where (:includeDeleted = true or t.deletedAt is null) order by t.name, t.id", DocumentTemplate.class)
                .setParameter("includeDeleted", includeDeleted)
                .setFirstResult(page * size).setMaxResults(size).getResultList();
    }
    @Override public java.util.Map<Long, Integer> latestNumbers(java.util.Collection<Long> ids) {
        if (ids.isEmpty()) { return java.util.Map.of(); }
        var result = new java.util.HashMap<Long, Integer>();
        em.createQuery("select v.id.templateId, max(v.id.number) from TemplateVersion v where v.id.templateId in :ids group by v.id.templateId", Object[].class)
                .setParameter("ids", ids).getResultList().forEach(row -> result.put((Long) row[0], (Integer) row[1]));
        return result;
    }
    @Override public List<DocumentTemplate> scopedTemplates(int page, int size, boolean includeDeleted, Long clientId, Long workId) {
        return em.createQuery("select t from DocumentTemplate t where (:deleted=true or t.deletedAt is null) "
                + "and t.clientId=:client and ((:work is null and t.workId is null) or t.workId=:work) order by t.name,t.id", DocumentTemplate.class)
                .setParameter("deleted", includeDeleted).setParameter("client", clientId).setParameter("work", workId)
                .setFirstResult(page * size).setMaxResults(size).getResultList();
    }
    @Override public List<DocumentPackage> scopedPackages(int page, int size, Long clientId, Long workId) {
        return em.createQuery("select p from DocumentPackage p where p.clientId=:client "
                + "and ((:work is null and p.workId is null) or p.workId=:work) order by p.name,p.id", DocumentPackage.class)
                .setParameter("client", clientId).setParameter("work", workId)
                .setFirstResult(page * size).setMaxResults(size).getResultList();
    }
    @Override public void add(Object entity) { em.persist(entity); }
    @Override public void flush() { em.flush(); }
    @Override public Optional<TemplateVersion> latest(Long id) {
        return em.createQuery("select v from TemplateVersion v where v.id.templateId = :id order by v.id.number desc", TemplateVersion.class)
                .setParameter("id", id).setMaxResults(1).getResultStream().findFirst();
    }
    @Override public TemplateVersion version(TemplateVersionId id) {
        var value = em.find(TemplateVersion.class, id);
        if (value == null) { throw missing(); }
        return value;
    }
    @Override public List<TemplateVersion> versions(Long id) {
        return em.createQuery("select v from TemplateVersion v where v.id.templateId = :id order by v.id.number desc", TemplateVersion.class)
                .setParameter("id", id).getResultList();
    }
    @Override public List<TemplateVersionField> versionFields(TemplateVersionId id) {
        return em.createQuery("select f from TemplateVersionField f where f.id.templateId = :id and f.id.number = :number order by f.id.fieldId", TemplateVersionField.class)
                .setParameter("id", id.templateId()).setParameter("number", id.number()).getResultList();
    }
    @Override public DocumentFile file(Long id) {
        var value = em.find(DocumentFile.class, id);
        if (value == null) { throw missing(); }
        return value;
    }
    @Override public Optional<RegistrationOperation> registration(UUID id, boolean lock) {
        var value = em.find(RegistrationOperation.class, id,
                lock ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE);
        if (lock && value != null) { em.refresh(value, LockModeType.PESSIMISTIC_WRITE); }
        return Optional.ofNullable(value);
    }
    @Override public DocumentPackage documentPackage(Long id, boolean lock) {
        var value = em.find(DocumentPackage.class, id, lock ? LockModeType.PESSIMISTIC_WRITE : LockModeType.NONE);
        if (value == null) { throw missing(); }
        return value;
    }
    @Override public List<DocumentPackage> packages(int page, int size, Long clientId, boolean confirmedOnly) {
        return em.createQuery("select p from DocumentPackage p where (:client is null or p.clientId = :client) "
                + "and (:confirmed = false or p.state = :state) order by p.name, p.id", DocumentPackage.class)
                .setParameter("client", clientId).setParameter("confirmed", confirmedOnly).setParameter("state", DocumentPackage.State.CONFIRMED)
                .setFirstResult(page * size).setMaxResults(size).getResultList();
    }
    @Override public void discard(DocumentPackage value) { em.remove(value); }
    @Override public boolean activeClient(Long id) {
        return em.createQuery("select count(c) from Client c where c.id = :id and c.deletedAt is null", Long.class)
                .setParameter("id", id).getSingleResult() > 0;
    }
    @Override public Optional<Long> workClient(Long id) {
        return em.createQuery("select w.client.id from Work w where w.id = :id and w.deletedAt is null and w.client.deletedAt is null", Long.class)
                .setParameter("id", id).getResultStream().findFirst();
    }
    private DocumentException missing() { return new DocumentException(404, "DOCUMENT_RESOURCE_NOT_FOUND", "No existe el recurso documental solicitado."); }
}
