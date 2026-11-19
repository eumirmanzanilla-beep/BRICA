package com.brica.extraction.application;
import com.brica.server.shared.validation.domain.ExtractionRules;

import com.brica.budgets.application.BudgetService;
import com.brica.clients.application.ClientRevision;
import com.brica.documents.application.*;
import com.brica.documents.application.port.ReferenceFieldStore;
import com.brica.documents.domain.*;
import com.brica.extraction.application.port.ExtractionStore;
import com.brica.extraction.domain.*;
import com.brica.server.security.application.AccessPolicy;
import com.brica.works.application.*;
import com.brica.works.application.port.WorkStore;
import com.brica.works.domain.Work;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service @Transactional(readOnly=true)
@PreAuthorize("@accessPolicy.hasPermission('WORKS_READ')")
public class ExtractionService {
    private final ExtractionStore store; private final WorkStore works; private final SourceEvidenceService evidence;
    private final AccessPolicy access; private final JsonMapper json; private final SelectiveInformationService changes;
    private final BudgetService budgets; private final ReferenceFieldStore fields;
    public ExtractionService(ExtractionStore store,WorkStore works,SourceEvidenceService evidence,AccessPolicy access,
            JsonMapper json,SelectiveInformationService changes,BudgetService budgets,ReferenceFieldStore fields) {
        this.store=store; this.works=works; this.evidence=evidence; this.access=access; this.json=json; this.changes=changes; this.budgets=budgets; this.fields=fields;
    }
    public record Create(UUID operationKey,List<Long> attachmentIds) { }
    public record Confirm(UUID operationKey,String target,UUID budgetBlockId) { }
    public record Confirmation(UUID id,String block,String actor,String result,java.time.OffsetDateTime confirmedAt) {
        static Confirmation from(ExtractionConfirmation c) { return new Confirmation(c.getId(),c.getBlockKey(),c.getActor(),c.getResult(),c.getConfirmedAt()); }
    }
    public record Summary(UUID id,Long workId,String state,String reviewState,String revision,String diagnostic,int confirmedBlocks,UUID parentJobId,String reviewClosedBy,java.time.OffsetDateTime reviewClosedAt) { }
    public record View(Summary job,ExtractionReview review,List<Long> attachmentIds,List<Confirmation> confirmations) { }
    public record Target(String target,String label,String type,String currentValue,String preferredSource) { }
    public record Context(String workRevision,String clientRevision,List<Target> targets) { }
    private Work work(Long id,boolean lock) {
        var w=(lock?works.findForUpdate(id):works.findById(id)).orElseThrow(()->new WorkNotFoundException(id));
        if(w.getClient().getDeletedAt()!=null) { throw new WorkNotFoundException(id); } return w;
    }
    private ExtractionJob job(UUID id,boolean lock) {
        var j=store.find(id,lock).orElseThrow(()->new DocumentException(404,"EXTRACTION_NOT_FOUND","No existe la extracción."));
        var w=work(j.getWorkId(),false);
        if(!w.getClient().getId().equals(j.getClientId())) { throw new DocumentException(409,"EXTRACTION_CONTEXT_CHANGED","El contexto original de la extracción cambió."); }
        return j;
    }
    private static void etag(ExtractionJob job,String expected) {
        if(expected==null || expected.isBlank()) { throw new MissingPreconditionException(); }
        if(!job.etag().equals(expected)) { throw new DocumentException(412,"STALE_EXTRACTION","El borrador o el análisis cambió; consulta su estado sin descartar tu captura."); }
    }
    private Summary summary(ExtractionJob job) { return new Summary(job.getId(),job.getWorkId(),job.getState().name(),job.getReviewState().name(),job.etag(),job.getDiagnostic(),job.getConfirmedBlocks(),job.getParentJobId(),job.getReviewClosedBy(),job.getReviewClosedAt()); }
    private View view(ExtractionJob job) { return new View(summary(job),job.getReview(),store.sources(job.getId()).stream().map(ExtractionSource::getAttachmentId).toList(),store.confirmations(job.getId()).stream().map(Confirmation::from).toList()); }
    public List<Summary> list(Long work,int page,int size) { BudgetService.pagination(page,size); work(work,false); return store.list(work,page,size).stream().map(this::summary).toList(); }
    public View get(UUID id) { return view(job(id,false)); }
    public record Attempt(UUID id,int number,String outcome,java.time.OffsetDateTime startedAt,java.time.OffsetDateTime heartbeatAt,
            java.time.OffsetDateTime finishedAt,String diagnostic,String resultHash,String readerVersion,boolean legacyImported) { }
    public List<Attempt> attempts(UUID id,int page,int size) {
        BudgetService.pagination(page,size); job(id,false);
        return store.attempts(id,page,size).stream().map(a->new Attempt(a.getId(),a.getNumber(),a.getOutcome().name(),a.getStartedAt(),a.getHeartbeatAt(),a.getFinishedAt(),a.getDiagnostic(),a.getResultHash(),a.getReaderVersion(),a.isLegacyImported())).toList();
    }
    public List<SourceResult.Fragment> fragments(UUID id,int page,int size) {
        BudgetService.pagination(page,size); return job(id,false).getResults().stream().flatMap(r->r.fragments().stream()).skip((long)page*size).limit(size).toList();
    }
    public List<SourcePrecedencePolicy.Suggestion> suggestions(UUID id,int page,int size) {
        BudgetService.pagination(page,size); var j=job(id,false); var classes=new HashMap<Long,SourcePrecedencePolicy.SourceClass>();
        for(var source:store.sources(id)) {
            var attachment=evidence.metadata(source); String name=attachment.getOriginalName().toLowerCase(Locale.ROOT);
            var type=attachment.getKind()==com.brica.works.domain.WorkAttachment.Kind.CONTRACT?SourcePrecedencePolicy.SourceClass.CONTRACT:
                    name.endsWith(".xls")||name.endsWith(".xlsx")?SourcePrecedencePolicy.SourceClass.EXCEL_BUDGET:SourcePrecedencePolicy.SourceClass.OTHER;
            classes.put(source.getAttachmentId(),type);
        }
        var candidates=new ArrayList<SourcePrecedencePolicy.Candidate>();
        j.getResults().forEach(r->candidates.addAll(RecognizeSourceLabels.propose(r,classes.getOrDefault(r.attachmentId(),SourcePrecedencePolicy.SourceClass.OTHER))));
        return SourcePrecedencePolicy.recommend(candidates).stream().skip((long)page*size).limit(size).toList();
    }
    public Context context(Long id) {
        var w=work(id,false); var source=new WorkReferenceData(w); var targets=new ArrayList<Target>();
        for(var a:ReferenceAttribute.values()) {
            if(Set.of(ReferenceAttribute.WORK_ID,ReferenceAttribute.WORK_CLIENT_ID,ReferenceAttribute.CLIENT_ID).contains(a)) { continue; }
            if(a.getOrigin()==ReferenceOrigin.CLIENT && !access.hasPermission("CLIENTS_READ")) { continue; }
            Object value=a==ReferenceAttribute.WORK_STATUS?w.getStatus().name():source.valueOf(a);
            String preferred=Set.of("WORK_CONTRACT_NUMBER","WORK_CONTRACTOR","WORK_START_DATE","WORK_END_DATE","WORK_CONTRACTED_AMOUNT","WORK_AMOUNT_BEFORE_TAX","WORK_TAX_AMOUNT").contains(a.name())?"CONTRACT":null;
            targets.add(new Target(a.name(),a.getLabel(),a.getType().name(),value==null?null:value.toString(),preferred));
        }
        for(int p=0;p<100;p++) {
            var page=fields.list(p,100);
            page.stream().filter(f->f.getOrigin()==ReferenceOrigin.ADDITIONAL).forEach(f->targets.add(new Target("ADDITIONAL:"+f.getKey(),f.getVisibleName(),f.getType().name(),w.getAdditionalValues().get(f.getKey()),null)));
            if(page.size()<100) { break; }
        }
        return new Context(WorkRevision.etag(w),access.hasPermission("CLIENTS_READ")?ClientRevision.etag(w.getClient()):null,List.copyOf(targets));
    }
    @Transactional @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ') and @accessPolicy.hasPermission('WORKS_WRITE')")
    public View create(Long workId,Create request) {
        return createJob(workId,request,null);
    }
    private View createJob(Long workId,Create request,UUID parentJobId) {
        ExtractionRules.require(request!=null && request.operationKey()!=null && request.attachmentIds()!=null && !request.attachmentIds().isEmpty()
                && request.attachmentIds().size()<=4 && new HashSet<>(request.attachmentIds()).size()==request.attachmentIds().size(),"sources","Selecciona de uno a cuatro adjuntos distintos y una clave de operación.");
        var w=work(workId,true); String actor=access.current().username(); String hash=DocumentHashes.sha256(json.writeValueAsString(Arrays.asList("EXTRACTION_CREATE_V1",request,parentJobId)));
        var previous=store.request(workId,actor,request.operationKey());
        if(previous.isPresent()) {
            if(!previous.get().getRequestHash().equals(hash)) { throw new DocumentException(409,"EXTRACTION_KEY_REUSED","La misma clave ya identifica otra solicitud."); }
            return view(previous.get());
        }
        var initial=ExtractionReview.empty(WorkRevision.etag(w),access.hasPermission("CLIENTS_READ")?ClientRevision.etag(w.getClient()):null);
        var j=store.save(new ExtractionJob(workId,w.getClient().getId(),request.operationKey(),hash,actor,initial,parentJobId));
        int order=0;
        for(Long id:request.attachmentIds()) {
            var a=evidence.active(workId,id);
            var s=new ExtractionSource(j.getId(),id,workId,order++,a.getSha256()); evidence.read(s); store.addSource(s);
        }
        store.save(j); return view(j);
    }
    @Transactional @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ') and @accessPolicy.hasPermission('WORKS_WRITE')")
    public View save(UUID id,String expected,ExtractionReview review) {
        var j=job(id,true); etag(j,expected); j.requireDraft(); bounded(review);
        var previous=j.getReview();
        for(var c:store.confirmations(id)) {
            if(c.getBlockKey().equals("WORK")) {
                ExtractionRules.require(Objects.equals(previous.workRevision(),review.workRevision()) && selected(previous,false).equals(selected(review,false)),"review","La obra ya confirmada no se modifica en este borrador.");
            } else if(c.getBlockKey().equals("CLIENT")) {
                ExtractionRules.require(Objects.equals(previous.clientRevision(),review.clientRevision()) && selected(previous,true).equals(selected(review,true)),"review","El cliente ya confirmado no se modifica en este borrador.");
            } else {
                var block=UUID.fromString(c.getBlockKey().substring(7));
                ExtractionRules.require(Objects.equals(budget(previous,block),budget(review,block)),"review","La revisión presupuestal confirmada es inmutable.");
            }
        }
        review.decisions().forEach(d->SelectiveInformationService.writable(d.target()));
        j.saveReview(review); return view(store.save(j));
    }
    private void bounded(ExtractionReview review) {
        ExtractionRules.require(review!=null && json.writeValueAsString(review).getBytes(StandardCharsets.UTF_8).length<=900_000,"review","El borrador supera el límite de tamaño.");
    }
    private static List<ExtractionReview.Decision> selected(ExtractionReview review,boolean client) { return review.decisions().stream().filter(d->SelectiveInformationService.clientTarget(d.target())==client).toList(); }
    private static com.brica.budgets.domain.BudgetDraft budget(ExtractionReview r,UUID block) { return r.budgets().stream().filter(b->b.blockId().equals(block)).findFirst().orElse(null); }
    @Transactional
    public Confirmation confirm(UUID id,String expected,Confirm request) {
        ExtractionRules.require(request!=null && request.operationKey()!=null && request.target()!=null && Set.of("WORK","CLIENT","BUDGET").contains(request.target()),"confirmation","Selecciona obra, cliente o presupuesto y una clave de operación.");
        boolean client=request.target().equals("CLIENT");
        if(!access.hasPermission(client?"CLIENTS_WRITE":"WORKS_WRITE") || client && !access.hasPermission("CLIENTS_READ")) { throw new AccessDeniedException("Sin permiso para este destino."); }
        var j=job(id,true); var r=j.getReview(); var selected=selected(r,client);
        Object intent=request.target().equals("BUDGET")?budget(r,request.budgetBlockId()):Arrays.asList(client?r.clientRevision():r.workRevision(),selected);
        String actor=access.current().username(); String hash=DocumentHashes.sha256(json.writeValueAsString(Arrays.asList(request.target(),request.budgetBlockId(),intent,actor)));
        var existing=store.confirmations(id).stream().filter(c->c.getOperationKey().equals(request.operationKey())).findFirst();
        if(existing.isPresent()) {
            if(!hash.equals(existing.get().getRequestHash())) { throw new DocumentException(409,"CONFIRMATION_KEY_REUSED","La clave ya identifica otra intención."); }
            return Confirmation.from(existing.get());
        }
        etag(j,expected); j.requireDraft();
        String block=request.target().equals("BUDGET")?"BUDGET:"+request.budgetBlockId():request.target();
        if(store.confirmations(id).stream().anyMatch(c->c.getBlockKey().equals(block))) { throw new DocumentException(409,"BLOCK_ALREADY_CONFIRMED","Este bloque ya fue confirmado; su historial no se sobrescribe."); }
        var sources=store.sources(id); sources.forEach(evidence::read);
        Set<UUID> evidenceIds=new HashSet<>(); j.getResults().forEach(s->s.fragments().forEach(f->evidenceIds.add(f.id())));
        UUID confirmationId=UUID.randomUUID(); String result; Object before;
        if(request.target().equals("BUDGET")) {
            var draft=budget(r,request.budgetBlockId()); ExtractionRules.require(draft!=null,"budget","Selecciona un presupuesto guardado en el borrador.");
            for(var item:draft.items()) { validateEvidence(item.evidenceId(),evidenceIds,draft.justification()); }
            before=draft.budgetId(); var confirmed=budgets.confirm(j.getWorkId(),j.getId(),confirmationId,draft);
            result="Presupuesto #"+confirmed.budgetId()+", revisión "+confirmed.number()+" ("+confirmed.id()+")";
        } else {
            ExtractionRules.require(!selected.isEmpty(),"decisions","Selecciona y guarda al menos un campo para este destino.");
            for(var d:selected) { validateEvidence(d.evidenceId(),evidenceIds,d.justification()); }
            before=context(j.getWorkId()).targets().stream().filter(t->selected.stream().anyMatch(d->d.target().equals(t.target()))).toList();
            String revision=client?changes.client(j.getClientId(),r.clientRevision(),selected):changes.work(j.getWorkId(),r.workRevision(),selected);
            result=(client?"Cliente #"+j.getClientId():"Obra #"+j.getWorkId())+" confirmado. Revisión "+revision;
        }
        String audit=json.writeValueAsString(Arrays.asList("SELECTIVE_CAPTURE_CONFIRMATION",before,intent,sources.stream().map(s->Arrays.asList(s.getAttachmentId(),s.getSha256())).toList()));
        var confirmation=new ExtractionConfirmation(confirmationId,id,request.operationKey(),hash,block,actor,result,audit);
        store.confirm(confirmation); j.recordConfirmation(); store.save(j); return Confirmation.from(confirmation);
    }
    private static void validateEvidence(UUID id,Set<UUID> known,String reason) {
        if(id!=null) { ExtractionRules.require(known.contains(id),"evidenceId","La evidencia no pertenece a este intento."); }
        else { ExtractionRules.text("justification",reason,2000,true); ExtractionRules.require(reason.strip().length()>=10,"justification","Justifica la captura manual sin fragmento (mínimo 10 caracteres)."); }
    }
    @Transactional @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ') and @accessPolicy.hasPermission('WORKS_WRITE')")
    public View reanalyze(UUID id,UUID operation) {
        var previous=job(id,false);
        // A new durable job retains the previous result, review and confirmations unchanged.
        return createJob(previous.getWorkId(),new Create(operation,store.sources(id).stream().map(ExtractionSource::getAttachmentId).toList()),id);
    }
    @Transactional @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ') and @accessPolicy.hasPermission('WORKS_WRITE')")
    public View cancel(UUID id,String expected) {
        var j=job(id,true); etag(j,expected); UUID token=j.getAttemptToken(); j.cancel(access.current().username());
        store.finishAttempt(token,ExtractionAttempt.Outcome.CANCELLED,"Cancelado expresamente por el usuario; no revierte bloques confirmados.",null,null);
        return view(store.save(j));
    }
    @Transactional @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ') and @accessPolicy.hasPermission('WORKS_WRITE')")
    public View close(UUID id,String expected) {
        var j=job(id,true); etag(j,expected); Set<String> required=new HashSet<>();
        if(!selected(j.getReview(),false).isEmpty()) { required.add("WORK"); }
        if(!selected(j.getReview(),true).isEmpty()) { required.add("CLIENT"); }
        j.getReview().budgets().forEach(b->required.add("BUDGET:"+b.blockId()));
        var completed=store.confirmations(id).stream().map(ExtractionConfirmation::getBlockKey).collect(java.util.stream.Collectors.toSet());
        ExtractionRules.require(!required.isEmpty() && completed.containsAll(required),"review","Quedan bloques seleccionados sin confirmar.");
        UUID token=j.getAttemptToken(); j.closeReview(access.current().username());
        store.finishAttempt(token,ExtractionAttempt.Outcome.CANCELLED,"El usuario finalizó la revisión; la lectura pendiente se detiene.",null,null);
        return view(store.save(j));
    }
}
