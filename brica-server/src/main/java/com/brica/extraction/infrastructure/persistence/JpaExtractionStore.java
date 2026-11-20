package com.brica.extraction.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import com.brica.extraction.application.port.ExtractionStore;
import com.brica.extraction.domain.*;

@Repository
public class JpaExtractionStore implements ExtractionStore {
    private final EntityManager em;
    public JpaExtractionStore(EntityManager em) { this.em = em; }
    @Override public ExtractionJob save(ExtractionJob job) {
        if (!em.contains(job)) { em.persist(job); }
        em.flush(); return job;
    }
    @Override public Optional<ExtractionJob> find(UUID id, boolean lock) {
        return Optional.ofNullable(lock ? em.find(ExtractionJob.class, id, LockModeType.PESSIMISTIC_WRITE) : em.find(ExtractionJob.class, id));
    }
    @Override public Optional<ExtractionJob> request(Long workId, String actor, UUID key) {
        return em.createQuery("select j from ExtractionJob j where j.workId=:work and j.actor=:actor and j.requestKey=:key", ExtractionJob.class)
                .setParameter("work", workId).setParameter("actor", actor).setParameter("key", key).getResultStream().findFirst();
    }
    @Override public List<ExtractionJob> list(Long workId, int page, int size) {
        return em.createQuery("select j from ExtractionJob j where j.workId=:work order by j.createdAt desc,j.id", ExtractionJob.class)
                .setParameter("work", workId).setFirstResult(Math.multiplyExact(page, size)).setMaxResults(size).getResultList();
    }
    @Override public List<UUID> available() {
        return em.createQuery("select j.id from ExtractionJob j where j.reviewState=:review and (j.state=:queued or (j.state=:running and j.leaseUntil<:now)) order by j.createdAt,j.id", UUID.class)
                .setParameter("review", ExtractionJob.ReviewState.DRAFT).setParameter("queued", ExtractionJob.State.QUEUED)
                .setParameter("running", ExtractionJob.State.RUNNING).setParameter("now", OffsetDateTime.now()).setMaxResults(1).getResultList();
    }
    @Override public boolean heartbeat(UUID id,UUID token) {
        // Renew only the lease, not the review ETag; stale executors cannot reclaim it.
        var now=OffsetDateTime.now();
        int renewed=em.createQuery("update ExtractionJob j set j.leaseUntil=:until where j.id=:id and j.attemptToken=:token and j.state=:running and j.reviewState=:draft and j.leaseUntil>:now")
                .setParameter("until",now.plusSeconds(30)).setParameter("id",id).setParameter("token",token)
                .setParameter("running",ExtractionJob.State.RUNNING).setParameter("draft",ExtractionJob.ReviewState.DRAFT).setParameter("now",now).executeUpdate();
        if(renewed==0) { return false; }
        int attempts=em.createQuery("update ExtractionAttempt a set a.heartbeatAt=:now where a.id=:token and a.jobId=:job and a.outcome=:running")
                .setParameter("now",now).setParameter("token",token).setParameter("job",id).setParameter("running",ExtractionAttempt.Outcome.RUNNING).executeUpdate();
        if(attempts!=1) { throw new IllegalStateException("Running extraction attempt history is missing"); }
        return true;
    }
    @Override public void startAttempt(ExtractionAttempt attempt) { em.persist(attempt); }
    @Override public void finishAttempt(UUID token,ExtractionAttempt.Outcome outcome,String diagnostic,String hash,String version) {
        if(token==null) { return; }
        em.createQuery("update ExtractionAttempt a set a.outcome=:outcome,a.finishedAt=:now,a.diagnostic=:diagnostic,a.resultHash=:hash,a.readerVersion=:version where a.id=:token and a.outcome=:running")
                .setParameter("outcome",outcome).setParameter("now",OffsetDateTime.now()).setParameter("diagnostic",diagnostic).setParameter("hash",hash).setParameter("version",version)
                .setParameter("token",token).setParameter("running",ExtractionAttempt.Outcome.RUNNING).executeUpdate();
    }
    @Override public List<ExtractionAttempt> attempts(UUID job,int page,int size) { return em.createQuery("select a from ExtractionAttempt a where a.jobId=:job order by a.number desc",ExtractionAttempt.class).setParameter("job",job).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList(); }
    @Override public void addSource(ExtractionSource source) { em.persist(source); }
    @Override public List<ExtractionSource> sources(UUID jobId) {
        return em.createQuery("select s from ExtractionSource s where s.jobId=:id order by s.order", ExtractionSource.class).setParameter("id", jobId).getResultList();
    }
    @Override public List<ExtractionConfirmation> confirmations(UUID jobId) {
        return em.createQuery("select c from ExtractionConfirmation c where c.jobId=:id order by c.confirmedAt,c.id", ExtractionConfirmation.class).setParameter("id", jobId).getResultList();
    }
    @Override public void confirm(ExtractionConfirmation confirmation) { em.persist(confirmation); em.flush(); }
    @Override public List<MappingProfile> profiles(Long work,int page,int size) { return em.createQuery("select p from MappingProfile p where p.workId=:work order by p.name,p.version desc",MappingProfile.class).setParameter("work",work).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList(); }
    @Override public int profileVersion(Long work,String name) { return em.createQuery("select coalesce(max(p.version),0) from MappingProfile p where p.workId=:work and p.name=:name",Integer.class).setParameter("work",work).setParameter("name",name).getSingleResult(); }
    @Override public void profile(MappingProfile profile) { em.persist(profile); em.flush(); }
}
