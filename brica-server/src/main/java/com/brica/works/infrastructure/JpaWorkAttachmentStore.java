package com.brica.works.infrastructure;

import com.brica.works.application.port.WorkAttachmentStore;
import com.brica.works.domain.WorkAttachment;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaWorkAttachmentStore implements WorkAttachmentStore {
    private final EntityManager em;
    public JpaWorkAttachmentStore(EntityManager em) { this.em = em; }
    public WorkAttachment save(WorkAttachment value) { em.persist(value); em.flush(); return value; }
    public List<WorkAttachment> list(Long workId, int page, int size) {
        return em.createQuery("select a from WorkAttachment a where a.workId=:work and a.deletedAt is null order by a.id", WorkAttachment.class)
                .setParameter("work", workId).setFirstResult(page * size).setMaxResults(size).getResultList();
    }
    public Optional<WorkAttachment> find(Long id, Long workId) {
        return em.createQuery("select a from WorkAttachment a where a.id=:id and a.workId=:work and a.deletedAt is null", WorkAttachment.class)
                .setParameter("id", id).setParameter("work", workId).getResultStream().findFirst();
    }
    public Optional<WorkAttachment> evidence(Long id, Long workId) {
        return em.createQuery("select a from WorkAttachment a where a.id=:id and a.workId=:work", WorkAttachment.class)
                .setParameter("id", id).setParameter("work", workId).getResultStream().findFirst();
    }
}
