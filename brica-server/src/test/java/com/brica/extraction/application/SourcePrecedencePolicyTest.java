package com.brica.extraction.application;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SourcePrecedencePolicyTest {
    private SourcePrecedencePolicy.Candidate candidate(String target,String value,SourcePrecedencePolicy.SourceClass source) { return new SourcePrecedencePolicy.Candidate(UUID.randomUUID(),1L,target,value,source); }
    @Test void recommendsContractAmountsButRetainsBudgetAlternatives() {
        var contract=candidate("WORK_CONTRACTED_AMOUNT","100.50",SourcePrecedencePolicy.SourceClass.CONTRACT);
        var budget=candidate("WORK_CONTRACTED_AMOUNT","120",SourcePrecedencePolicy.SourceClass.EXCEL_BUDGET);
        var result=SourcePrecedencePolicy.recommend(List.of(contract,budget));
        assertEquals(2,result.size()); assertTrue(result.getFirst().recommended()); assertFalse(result.get(1).recommended());
        assertEquals("120",result.get(1).proposedValue());
    }
    @Test void contradictoryPreferredSourcesAndNamesRemainUnresolved() {
        var result=SourcePrecedencePolicy.recommend(List.of(candidate("WORK_START_DATE","03/04/2026",SourcePrecedencePolicy.SourceClass.CONTRACT),
                candidate("WORK_START_DATE","04/03/2026",SourcePrecedencePolicy.SourceClass.CONTRACT),candidate("WORK_DOCUMENT_NAME","Nombre A",SourcePrecedencePolicy.SourceClass.CONTRACT)));
        assertTrue(result.stream().noneMatch(SourcePrecedencePolicy.Suggestion::recommended));
        assertEquals("03/04/2026",result.getFirst().proposedValue());
    }
    @Test void prefersSelectedExcelBudgetForConceptsAndNeverTreatsAbsenceAsZero() {
        var result=SourcePrecedencePolicy.recommend(List.of(candidate("BUDGET_ITEM_PRICE",null,SourcePrecedencePolicy.SourceClass.EXCEL_BUDGET),
                candidate("BUDGET_ITEM_PRICE","0",SourcePrecedencePolicy.SourceClass.EXCEL_BUDGET),candidate("BUDGET_ITEM_PRICE","10",SourcePrecedencePolicy.SourceClass.OTHER)));
        assertFalse(result.getFirst().recommended()); assertTrue(result.get(1).recommended()); assertFalse(result.get(2).recommended());
    }
}
