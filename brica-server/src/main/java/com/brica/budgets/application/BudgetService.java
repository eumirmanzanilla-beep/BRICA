package com.brica.budgets.application;

import com.brica.budgets.application.port.BudgetStore;
import com.brica.budgets.domain.*;
import com.brica.documents.application.DocumentException;
import com.brica.works.application.WorkNotFoundException;
import com.brica.works.application.port.WorkStore;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @Transactional(readOnly = true)
@PreAuthorize("@accessPolicy.hasPermission('WORKS_READ')")
public class BudgetService {
    private final BudgetStore store; private final WorkStore works;
    public BudgetService(BudgetStore store, WorkStore works) { this.store = store; this.works = works; }
    public record View(Long id, Long workId, String name, UUID principalRevisionId, String revision) {
        static View from(Budget b) { return new View(b.getId(),b.getWorkId(),b.getName(),b.getPrincipalRevisionId(),b.etag()); }
    }
    public record Revision(UUID id, Long budgetId, int number, UUID sourceJobId, String currency, BigDecimal subtotal,
            BigDecimal tax, BigDecimal total, String controlPolicy, List<String> notes,
            List<BudgetDraft.Difference> differences, String justification) {
        static Revision from(BudgetRevision r) { return new Revision(r.getId(),r.getBudgetId(),r.getNumber(),r.getJobId(),r.getCurrency(),r.getSubtotal(),r.getTax(),r.getTotal(),r.getControlPolicy(),r.getNotes(),r.getDifferences(),r.getJustification()); }
    }
    public record Section(UUID id,int position,String title) { }
    public record Item(UUID id,UUID sectionId,int position,String code,String description,String unit,BigDecimal quantity,BigDecimal price,BigDecimal amount,UUID evidenceId,String originalMeasurement) { }
    private void work(Long id) { var work=works.findById(id).orElseThrow(()->new WorkNotFoundException(id)); if(work.getClient().getDeletedAt()!=null) { throw new WorkNotFoundException(id); } }
    private Budget budget(Long id,boolean lock) { var b=store.find(id,lock).orElseThrow(()->new DocumentException(404,"BUDGET_NOT_FOUND","No existe el presupuesto.")); work(b.getWorkId()); return b; }
    private BudgetRevision revision(UUID id) { var r=store.revision(id).orElseThrow(()->new DocumentException(404,"BUDGET_REVISION_NOT_FOUND","No existe la revisión.")); budget(r.getBudgetId(),false); return r; }
    public List<View> list(Long work,int page,int size) { pagination(page,size); work(work); return store.list(work,page,size).stream().map(View::from).toList(); }
    public List<Revision> revisions(Long budget,int page,int size) { pagination(page,size); budget(budget,false); return store.revisions(budget,page,size).stream().map(Revision::from).toList(); }
    public List<Section> sections(UUID revision) { revision(revision); return store.sections(revision).stream().map(s->new Section(s.getId(),s.getPosition(),s.getTitle())).toList(); }
    public List<Item> items(UUID revision,int page,int size) { pagination(page,size); revision(revision); return store.items(revision,page,size).stream().map(i->new Item(i.getId(),i.getSectionId(),i.getPosition(),i.getCode(),i.getDescription(),i.getUnit(),i.getQuantity(),i.getPrice(),i.getAmount(),i.getEvidenceId(),i.getOriginalMeasurement())).toList(); }
    @Transactional @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ') and @accessPolicy.hasPermission('WORKS_WRITE')")
    public View principal(Long id,UUID revision,String expected) {
        com.brica.server.shared.validation.domain.ExtractionRules.require(revision!=null,"revisionId","Selecciona una revisión confirmada.");
        var b=budget(id,true); check(b,expected);
        var r=revision(revision);
        if (!r.getBudgetId().equals(id)) { throw new DocumentException(400,"INVALID_PRINCIPAL_REVISION","La revisión pertenece a otro presupuesto."); }
        b.selectPrincipal(revision); return View.from(store.save(b));
    }
    @Transactional @PreAuthorize("@accessPolicy.hasPermission('WORKS_READ') and @accessPolicy.hasPermission('WORKS_WRITE')")
    public Revision confirm(Long work,UUID job,UUID confirmation,BudgetDraft draft) {
        work(work); draft.validate();
        var b=draft.budgetId()==null ? store.save(new Budget(work,draft.name())) : budget(draft.budgetId(),true);
        if (!b.getWorkId().equals(work)) { throw new DocumentException(400,"INVALID_BUDGET_WORK","El presupuesto pertenece a otra obra."); }
        if (draft.budgetId()!=null) { check(b,draft.expectedRevision()); }
        var revision=new BudgetRevision(b.getId(),work,b.allocateNumber(),job,confirmation,draft);
        var sections=new ArrayList<BudgetSection>(); var ids=new HashMap<UUID,UUID>();
        for(var section:draft.sections()) { var saved=new BudgetSection(revision.getId(),section); sections.add(saved); ids.put(section.id(),saved.getId()); }
        var items=draft.items().stream().map(i->new BudgetItem(revision.getId(),ids.get(i.sectionId()),i)).toList();
        store.add(revision,sections,items); store.save(b); return Revision.from(revision);
    }
    private static void check(Budget b,String expected) {
        if(expected==null) { throw new com.brica.works.application.MissingPreconditionException(); }
        if(!b.etag().equals(expected)) { throw new DocumentException(412,"STALE_BUDGET","El presupuesto cambió; consulta su revisión vigente."); }
    }
    public static void pagination(int page,int size) {
        if(page<0 || page>10000 || size<1 || size>100) { throw new DocumentException(400,"INVALID_PAGE","Usa páginas de entre 1 y 100 registros."); }
    }
}
