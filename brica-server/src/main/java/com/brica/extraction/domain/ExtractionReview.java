package com.brica.extraction.domain;
import com.brica.server.shared.validation.domain.ExtractionRules;

import java.util.*;
import com.brica.budgets.domain.BudgetDraft;

public record ExtractionReview(String workRevision, String clientRevision,
        List<Decision> decisions, List<BudgetDraft> budgets) {
    public record Decision(String target, String value, UUID evidenceId, String justification, boolean correctionOfCapture) { }
    public ExtractionReview {
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        budgets = budgets == null ? List.of() : List.copyOf(budgets);
        ExtractionRules.require(decisions.size() <= 200 && budgets.size() <= 20, "review", "Máximo 200 decisiones y 20 presupuestos por borrador.");
        Set<UUID> blocks = new HashSet<>();
        for (var budget : budgets) { ExtractionRules.require(budget != null && budget.blockId() != null && blocks.add(budget.blockId()), "budget", "Los bloques de presupuesto requieren identidades únicas."); }
        Set<String> targets = new HashSet<>();
        for (var decision : decisions) {
            ExtractionRules.require(decision != null && decision.target() != null && targets.add(decision.target()), "target", "Selecciona cada destino una sola vez.");
            ExtractionRules.text("value", decision.value(), 16384, false);
            ExtractionRules.text("justification", decision.justification(), 2000, false);
        }
    }
    public static ExtractionReview empty(String workRevision, String clientRevision) {
        return new ExtractionReview(workRevision, clientRevision, List.of(), List.of());
    }
}
