package com.brica.extraction.application;
import com.brica.extraction.application.port.*;
import com.brica.extraction.domain.*;
import com.brica.server.security.application.port.AccountStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import com.brica.works.application.port.WorkStore;
import tools.jackson.databind.json.JsonMapper;
import com.brica.documents.application.DocumentHashes;

/** Short durable transactions; never runs a reader while holding a database lock. */
@Service
public class ExtractionExecutionService {
    private final ExtractionStore store; private final SourceEvidenceService evidence; private final AccountStore accounts; private final WorkStore works; private final JsonMapper json;
    public ExtractionExecutionService(ExtractionStore store,SourceEvidenceService evidence,AccountStore accounts,WorkStore works,JsonMapper json) { this.store=store; this.evidence=evidence; this.accounts=accounts; this.works=works; this.json=json; }
    public record Claim(UUID jobId,UUID token,List<SourceEvidenceService.Content> sources,AtomicLong nextHeartbeat) { }
    private boolean authorized(ExtractionJob job) {
        return accounts.findAccess(job.getActor()).filter(a->a.enabled() && a.permissions().containsAll(Set.of("WORKS_READ","WORKS_WRITE"))).isPresent()
                && works.findById(job.getWorkId()).filter(w->w.getClient().getDeletedAt()==null && w.getClient().getId().equals(job.getClientId())).isPresent();
    }
    @Transactional public Claim claim() {
        var available=store.available(); if(available.isEmpty()) { return null; }
        var job=store.find(available.getFirst(),true).orElseThrow();
        UUID previous=job.getAttemptToken();
        UUID token;
        try { token=job.claim(); } catch(com.brica.server.shared.validation.domain.InvalidDataException busy) { return null; }
        if(previous!=null) { store.finishAttempt(previous,ExtractionAttempt.Outcome.EXPIRED,"El arrendamiento venció antes de recuperar el trabajo.",null,null); }
        store.startAttempt(new ExtractionAttempt(token,job.getId(),job.getAttemptNumber()));
        if(!authorized(job) || job.getAttemptNumber()>3) {
            job.finish(token,List.of(),"El permiso fue retirado o se agotaron los reintentos de lectura.");
            store.finishAttempt(token,ExtractionAttempt.Outcome.FAILED,job.getDiagnostic(),null,null); store.save(job); return null;
        }
        try {
            var content=store.sources(job.getId()).stream().map(evidence::read).toList(); store.save(job);
            return new Claim(job.getId(),token,content,new AtomicLong(System.nanoTime()+TimeUnit.SECONDS.toNanos(10)));
        } catch(Exception failure) { job.finish(token,List.of(),"No se pudo verificar la fuente original."); store.finishAttempt(token,ExtractionAttempt.Outcome.FAILED,job.getDiagnostic(),null,null); store.save(job); return null; }
    }
    @Transactional public boolean active(Claim claim) {
        var job=store.find(claim.jobId(),false).filter(j->j.owns(claim.token()));
        if(job.isEmpty()) { return false; }
        if(System.nanoTime()>=claim.nextHeartbeat().get()) {
            if(!authorized(job.get()) || !store.heartbeat(claim.jobId(),claim.token())) { return false; }
            claim.nextHeartbeat().set(System.nanoTime()+TimeUnit.SECONDS.toNanos(10));
        }
        return true;
    }
    @Transactional public void publish(Claim claim,SourceProcessor.Result result) {
        var job=store.find(claim.jobId(),true).orElseThrow();
        if(!job.owns(claim.token())) { return; }
        var expected=new HashMap<Long,String>(); claim.sources().forEach(s->expected.put(s.id(),s.sha256()));
        var ids=new HashSet<Long>(); var fragments=new HashSet<UUID>();
        boolean valid=result.sources().stream().allMatch(s->ids.add(s.attachmentId()) && expected.containsKey(s.attachmentId()) && Objects.equals(expected.get(s.attachmentId()),s.sha256())
                && s.fragments().stream().allMatch(f->f.id()!=null && fragments.add(f.id()) && f.location()!=null && f.attachmentId()==s.attachmentId()));
        if(!valid || !authorized(job)) { job.finish(claim.token(),List.of(),"El resultado o los permisos ya no corresponden al intento vigente."); }
        else {
            String diagnostic=result.diagnostic();
            if(diagnostic==null && ids.size()!=expected.size()) { diagnostic="Faltan resultados de una o más fuentes; no se considera un análisis completo."; }
            job.finish(claim.token(),result.sources(),diagnostic);
        }
        if(job.getState()!=ExtractionJob.State.RUNNING) {
            String hash=DocumentHashes.sha256(json.writeValueAsString(job.getResults()));
            String versions=job.getResults().stream().map(SourceResult::readerVersion).distinct().sorted().collect(java.util.stream.Collectors.joining(","));
            store.finishAttempt(claim.token(),ExtractionAttempt.Outcome.valueOf(job.getState().name()),job.getDiagnostic(),hash,versions.isEmpty()?null:versions);
        }
        store.save(job);
    }
}
