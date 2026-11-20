package com.brica.budgets.infrastructure;
import com.brica.budgets.application.port.BudgetStore;
import com.brica.budgets.domain.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public class JpaBudgetStore implements BudgetStore {
    private final EntityManager em;
    public JpaBudgetStore(EntityManager em) { this.em = em; }
    public Budget save(Budget value) { if (value.getId() == null) { em.persist(value); } em.flush(); return value; }
    public Optional<Budget> find(Long id, boolean lock) { return Optional.ofNullable(lock ? em.find(Budget.class, id, LockModeType.PESSIMISTIC_WRITE) : em.find(Budget.class, id)); }
    public List<Budget> list(Long work, int page, int size) { return em.createQuery("select b from Budget b where b.workId=:work order by b.id", Budget.class).setParameter("work", work).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList(); }
    public void add(BudgetRevision revision, List<BudgetSection> sections, List<BudgetItem> items) {
        em.persist(revision); sections.forEach(em::persist); items.forEach(em::persist); em.flush();
    }
    public Optional<BudgetRevision> revision(UUID id) { return Optional.ofNullable(em.find(BudgetRevision.class,id)); }
    public List<BudgetRevision> revisions(Long budget, int page, int size) { return em.createQuery("select r from BudgetRevision r where r.budgetId=:budget order by r.number desc", BudgetRevision.class).setParameter("budget",budget).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList(); }
    public List<BudgetSection> sections(UUID id) { return em.createQuery("select s from BudgetSection s where s.revisionId=:id order by s.position,s.id",BudgetSection.class).setParameter("id",id).setMaxResults(200).getResultList(); }
    public List<BudgetItem> items(UUID id,int page,int size) { return em.createQuery("select i from BudgetItem i where i.revisionId=:id order by i.position,i.id",BudgetItem.class).setParameter("id",id).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList(); }
}
