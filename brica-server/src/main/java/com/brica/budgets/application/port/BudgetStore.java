package com.brica.budgets.application.port;
import com.brica.budgets.domain.*;
import java.util.*;

public interface BudgetStore {
    Budget save(Budget budget);
    Optional<Budget> find(Long id, boolean lock);
    List<Budget> list(Long workId, int page, int size);
    void add(BudgetRevision revision, List<BudgetSection> sections, List<BudgetItem> items);
    Optional<BudgetRevision> revision(UUID id);
    List<BudgetRevision> revisions(Long budgetId, int page, int size);
    List<BudgetSection> sections(UUID revisionId);
    List<BudgetItem> items(UUID revisionId, int page, int size);
}
