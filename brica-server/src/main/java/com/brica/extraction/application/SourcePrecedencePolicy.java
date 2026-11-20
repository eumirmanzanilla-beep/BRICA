package com.brica.extraction.application;

import java.util.*;

/** Versioned recommendation only; never selects, confirms or overwrites a value. */
public final class SourcePrecedencePolicy {
    public static final String VERSION="PRECEDENCE_V1";
    public enum SourceClass { CONTRACT, EXCEL_BUDGET, OTHER }
    public record Candidate(UUID fragmentId,long attachmentId,String target,String value,SourceClass source) { }
    public record Suggestion(UUID fragmentId,long attachmentId,String target,String proposedValue,boolean recommended,String reason) { }
    private static final Set<String> CONTRACT=Set.of("WORK_CONTRACT_NUMBER","WORK_CONTRACTOR","WORK_START_DATE","WORK_END_DATE","WORK_CONTRACTED_AMOUNT","WORK_AMOUNT_BEFORE_TAX","WORK_TAX_AMOUNT");
    private static final Set<String> BUDGET=Set.of("BUDGET_ITEM_CODE","BUDGET_ITEM_DESCRIPTION","BUDGET_ITEM_UNIT","BUDGET_ITEM_QUANTITY","BUDGET_ITEM_PRICE","BUDGET_SECTION");
    private SourcePrecedencePolicy() { }
    public static List<Suggestion> recommend(List<Candidate> candidates) {
        var grouped=new LinkedHashMap<String,List<Candidate>>();
        candidates.forEach(c->grouped.computeIfAbsent(c.target(),ignored->new ArrayList<>()).add(c));
        var result=new ArrayList<Suggestion>();
        grouped.forEach((target,values)->{
            SourceClass preferred=CONTRACT.contains(target)?SourceClass.CONTRACT:BUDGET.contains(target)?SourceClass.EXCEL_BUDGET:null;
            var preferredValues=preferred==null?Set.<String>of():values.stream().filter(c->c.source()==preferred && c.value()!=null && !c.value().isBlank()).map(Candidate::value).collect(java.util.stream.Collectors.toSet());
            for(var candidate:values) {
                boolean recommended=preferred!=null && preferredValues.size()==1 && candidate.source()==preferred && preferredValues.contains(candidate.value());
                String reason=preferredValues.size()>1?"Conflicto entre fuentes preferidas; elige expresamente.":recommended?"Recomendación "+VERSION+"; requiere revisión y confirmación humana.":"Alternativa conservada; sin selección automática.";
                result.add(new Suggestion(candidate.fragmentId(),candidate.attachmentId(),target,candidate.value(),recommended,reason));
            }
        });
        return List.copyOf(result);
    }
}
